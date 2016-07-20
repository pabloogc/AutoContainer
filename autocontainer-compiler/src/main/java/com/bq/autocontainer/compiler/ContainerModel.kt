package com.bq.autocontainer.compiler

import com.bq.autocontainer.AutoContainer
import com.bq.autocontainer.compiler.ProcessorUtils.env
import com.squareup.javapoet.*
import java.util.*
import javax.inject.Inject
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

class ContainerModel(
      val componentModel: ComponentModel,
      originalClassName: ClassName,
      val element: TypeElement,
      val plugins: List<PluginModel>) {

   val baseClass: TypeMirror
   val containerClassName: ClassName
   val baseClassElement: TypeElement
   val callbackMap: HashMap<ExecutableElement, MutableList<PluginModel.CallbackMethod>>
   val activityMethods: List<ExecutableElement>

   init {


      val annotation = element.getAnnotation(AutoContainer::class.java)
      val className =
            if (annotation.className.isEmpty()) originalClassName.simpleName() + "Container"
            else annotation.className

      containerClassName = ClassName.get(
            originalClassName.packageName(),
            className)

      baseClass = annotation.typeMirror(AutoContainer::baseClass)

      baseClassElement = baseClass.asElement() as TypeElement

      callbackMap = HashMap()
      activityMethods = baseClassElement.enclosedAndInheritedElements()
            .filter { it.kind == ElementKind.METHOD }
            .map { it as ExecutableElement }

      plugins.forEach { p ->
         p.callbackMethods.forEach { callback ->
            val match = activityMethods.firstOrNull { callback.matchesActivityMethod(it) }
            if (match == null) {
               logError("Not matching method found for $callback", callback.callbackMethod)
            } else {
               callbackMap.getOrPut(match, { ArrayList() }).add(callback)
            }
         }
      }

      callbackMap.forEach { executableElement, callbacks ->
         callbacks.sortWith(Comparator<PluginModel.CallbackMethod> { o1, o2 ->
            var out = o1.priority - o2.priority
            if (out == 0) { //Same priority
               out = o1.plugin.className.toString().compareTo(o2.plugin.className.toString())
            }
            out

         })
      }
   }

   fun generateClass() {
      val componentTypeSpec = TypeSpec.classBuilder(containerClassName)
            .superclass(ClassName.get(baseClass))
            .addModifiers(Modifier.PUBLIC)

      val initMethod = MethodSpec.methodBuilder("init")
            .addParameter(componentModel.componentClassName, "component")
            .addStatement("component.inject(this)")

      //Add a injection field for every plugin
      plugins.forEach {
         componentTypeSpec.addField(FieldSpec.builder(it.element.asType().toTypeName(), it.fieldName)
               .addAnnotation(Inject::class.java)
               .addAnnotations(it.declaringMethod.copyAnnotations().asIterable())
               .build())
         initMethod.addStatement("component.inject(this.${it.fieldName})")
      }

      componentTypeSpec.addMethod(initMethod.build())

      //Add a CallbackMethod for every callback
      callbackMap.forEach { methodToOverride, pluginCallbacksToInvoke ->

         val callbackClassRequired = pluginCallbacksToInvoke.any { it.canOverrideActivityMethod }
         val callbacksBeforeSuper = pluginCallbacksToInvoke
               .filter { it.callSuper == PluginModel.CallSuperType.AFTER }
         val callbacksAfterSuper = pluginCallbacksToInvoke
               .filter { it.callSuper == PluginModel.CallSuperType.BEFORE }

         val auxVarName = "result"
         val auxVarRequired = !methodToOverride.isVoid() && (callbacksAfterSuper.isNotEmpty() || callbackClassRequired)
         val auxVarType = TypeName.get(methodToOverride.returnType)

         val callbackMethodFieldName =
               if (callbackClassRequired) componentTypeSpec.let {
                  val field = generateCallbackAnonymousClass(methodToOverride)
                  it.addField(field)
                  field.name
               } else {
                  "NULL"
               }

         componentTypeSpec.addMethod(
               MethodSpec.overriding(methodToOverride).apply {

                  fun addPluginCall(cb: PluginModel.CallbackMethod) {
                     if (cb.canOverrideActivityMethod) {
                        val params = (listOf(callbackMethodFieldName)
                              .plus(methodToOverride.parameters.map { it.simpleName }))
                              .joinToString(", ")

                        //Borrow and call, they go to the same line to avoid noise in generated code
                        addCode("${methodToOverride.simpleName}.borrow(${cb.plugin.fieldName}); ")
                        addCode("this.${cb.plugin.fieldName}.${methodToOverride.simpleName}($params);\n")

                     } else {
                        addStatement("this.${cb.plugin.fieldName}.${methodToOverride.toInvocationString()}")
                     }
                  }

                  //Add all method that go before super
                  callbacksBeforeSuper.forEach { addPluginCall(it) }
                  if (callbacksBeforeSuper.isNotEmpty()) breakLine()

                  //Add super invocation, checking overridden value
                  if (auxVarRequired) {
                     if (callbackClassRequired) {
                        addStatement("final \$T $auxVarName", auxVarType)
                        addStatement("if(this.$callbackMethodFieldName.overridden()) $auxVarName = $callbackMethodFieldName.getOverriddenValue()")
                        addStatement("else $auxVarName = super.${methodToOverride.toInvocationString()}")
                     } else {
                        addStatement("final \$T $auxVarName = super.${methodToOverride.toInvocationString()}", auxVarType)
                     }
                  } else {
                     if (callbackClassRequired) {
                        addStatement("if(!this.$callbackMethodFieldName.overridden()) super.${methodToOverride.toInvocationString()}")
                     } else {
                        if (callbacksAfterSuper.isEmpty()) {
                           //Last line of the method
                           addStatement("return super.${methodToOverride.toInvocationString()}")
                        } else {
                           addStatement("super.${methodToOverride.toInvocationString()}")
                        }
                     }
                  }

                  //Add all method that go after super
                  callbacksAfterSuper.forEach { addPluginCall(it) }

                  //Reset any borrowing state in the method invocation, if needed
                  if (callbackClassRequired) {
                     breakLine()
                     addStatement("this.$callbackMethodFieldName.reset()")
                  }

                  //Return the aux value, if needed
                  if (auxVarRequired) {
                     addStatement("return $auxVarName")
                  }

               }.build()
         )
      }

      val file = JavaFile.builder(containerClassName.packageName(), componentTypeSpec.build()).build()
      file.writeTo(env.filer)
   }

   private fun generateCallbackAnonymousClass(methodToOverride: ExecutableElement): FieldSpec {

      val isVoidMethod = methodToOverride.returnType.kind == TypeKind.VOID
      val returnTypeName = TypeName.get(methodToOverride.returnType)

      val activityMethodElement = elementForName(CALLBACK_METHOD_CLASS_NAME).asType().asElement().asTypeElement()
      val callbackRawClassName = ClassName.get(activityMethodElement)
      val callbackTypeName = ParameterizedTypeName.get(callbackRawClassName, returnTypeName.box())

      val callContainerMethod = MethodSpec.methodBuilder("call")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addAnnotation(Override::class.java)
            .apply {

               val paramTypeNames = methodToOverride.parameters
                     .map { it.asType().toTypeName() }
                     .toTypedArray()

               val invocationStatement = methodToOverride.parameters.mapIndexed { i, param ->
                  if (i < 5) {
                     //Add the matching parameter to the method
                     addParameter(Any::class.java, param.simpleName.toString())
                     "(\$T)${param.simpleName}" //(Object)arg0
                  } else {
                     if (i == 5) {
                        //Add the array parameter to the method, only once
                        addParameter(ArrayTypeName.get(Any::class.java), "extra$i")
                     }
                     "(\$T)extra[${i - 5}" //(Object) arg5[0]
                  }
               }.joinToString(", ")

               // Add the statement to call the container method
               // Container.this.onBackPressed()
               if (isVoidMethod) {
                  addStatement("${containerClassName.simpleName()}.super." +
                        "${methodToOverride.simpleName}($invocationStatement)", *paramTypeNames)
                  addStatement("return null")
               } else {
                  addStatement("return ${containerClassName.simpleName()}.super." +
                        "${methodToOverride.simpleName}($invocationStatement)", *paramTypeNames)
               }
            }
            .returns(returnTypeName.box())

      // Wrap everything inside an anonymous class and generate the field for it
      val callbackClassAnonymousDeclaration = TypeSpec.anonymousClassBuilder("")
            .superclass(callbackTypeName)
            .addMethod(callContainerMethod.build())

      val callbackMethodField = FieldSpec.builder(callbackTypeName, methodToOverride.simpleName.toString())
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .initializer(CodeBlock.of("\$L", callbackClassAnonymousDeclaration.build()))
            .build()

      return callbackMethodField
   }
}
