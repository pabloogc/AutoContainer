package com.bq.autoactivity.compiler

import com.bq.autoactivity.ActivityCallback
import com.bq.autoactivity.AutoActivity
import com.bq.autoactivity.compiler.*
import com.bq.autoactivity.compiler.AutoActivityProcessor.env
import com.squareup.javapoet.*
import java.util.*
import javax.inject.Inject
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

class ActivityModel(
      originalClassName: ClassName,
      val element: TypeElement,
      val plugins: List<PluginModel>) {

   val baseClass: TypeMirror
   val activityClassName: ClassName
   val baseClassElement: TypeElement
   val callbackMap: HashMap<ExecutableElement, MutableList<PluginModel.CallbackMethod>>
   val activityMethods: List<ExecutableElement>

   init {
      activityClassName = ClassName.get(
            originalClassName.packageName(),
            originalClassName.simpleName() + "Activity")

      val annotation = element.getAnnotation(AutoActivity::class.java)
      baseClass = annotation.typeMirror(AutoActivity::baseActivity)

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
   }

   fun generateClass() {
      val componentTypeSpec = TypeSpec.classBuilder(activityClassName)
            .superclass(ClassName.get(baseClass))
            .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)

      //Add a injection point for every plugin
      plugins.forEach {
         componentTypeSpec.addField(FieldSpec.builder(it.element.asType().toTypeName(), it.fieldName)
               .addAnnotation(Inject::class.java)
               .addAnnotations(it.declaringMethod.copyAnnotations().asIterable())
               .build())
      }

      //Add a ActivityMethod for every callback
      callbackMap.forEach { methodToOverride, pluginCallbacksToInvoke ->
         val callbackClassRequired = pluginCallbacksToInvoke.any { it.canOverrideActivityMethod }
         val callbacksBeforeSuper = pluginCallbacksToInvoke
               .filter { it.callSuper == PluginModel.CallSuperType.AFTER }
         val callbacksAfterSuper = pluginCallbacksToInvoke
               .filter { it.callSuper == PluginModel.CallSuperType.BEFORE }

         val callbackMethodField =
               if (callbackClassRequired) componentTypeSpec.let {
                  val field = generateCallbackClass(methodToOverride)
                  it.addField(field)
                  field
               }
               else null

         componentTypeSpec.addMethod(
               MethodSpec.overriding(methodToOverride).apply {

                  fun addCallbackCall(m: PluginModel.CallbackMethod) {
                     if (m.canOverrideActivityMethod) {
                        addCode("\$L.borrow(\$N); this.\$L.\$L(\$L);\n",
                              methodToOverride.simpleName,
                              m.plugin.fieldName,
                              m.plugin.fieldName,
                              methodToOverride.simpleName,
                              (listOf(callbackMethodField!!.name)
                                    .plus(methodToOverride.parameters.map { it.simpleName }))
                                    .joinToString(", "))
                     } else {
                        addStatement("this.\$L.\$L", m.plugin.fieldName, methodToOverride.toInvocationString())
                     }
                  }

                  if (callbackClassRequired && methodToOverride.parameters.isNotEmpty()) {
                     addStatement("this.\$N.captureArguments(\$L)", callbackMethodField,
                           methodToOverride.parameters.map { it.simpleName }.joinToString(", "))
                  }

                  //Add all method that go before super
                  callbacksBeforeSuper.forEach { addCallbackCall(it) }

                  addCode("//Super method invocation\n")

                  //Add aux return value if needed
                  if (!methodToOverride.isVoid()) {
                     addStatement("final \$T returnedValue", TypeName.get(methodToOverride.returnType))
                  }

                  if (callbackClassRequired) {

                     if (methodToOverride.isVoid()) {
                        addStatement("if(!this.\$N.overridden()) super.\$L", callbackMethodField, methodToOverride.toInvocationString())
                     } else {
                        addStatement("if(this.\$N.overridden()) returnedValue = \$N.getOverriddenValue()", callbackMethodField, callbackMethodField)
                        addStatement("else returnedValue = super.\$L", methodToOverride.toInvocationString())
                     }

                  } else { //No callback class

                     if (methodToOverride.isVoid()) {
                        addStatement("super.\$L", methodToOverride.toInvocationString())
                     } else {
                        addStatement("returnedValue = super.\$L", methodToOverride.toInvocationString())
                     }
                  }
                  addCode("//End super method invocation\n")

                  //Add all method that go after super
                  callbacksAfterSuper.forEach { addCallbackCall(it) }

                  //Exit
                  if (callbackClassRequired) {
                     addStatement("this.\$N.reset()", callbackMethodField)
                  }
                  if (!methodToOverride.isVoid()) {
                     addStatement("return returnedValue")
                  }

               }.build()
         )
      }

      val file = JavaFile.builder(activityClassName.packageName(), componentTypeSpec.build()).build()
      file.writeTo(env.filer)
   }

   private fun generateCallbackClass(methodToOverride: ExecutableElement): FieldSpec {

      val isVoidMethod = methodToOverride.returnType.kind == TypeKind.VOID
      val returnTypeName = TypeName.get(methodToOverride.returnType)
      val callbackClassElement = elementForName(ACTIVITY_METHOD_CLASS_NAME)
      val captureArgumentsCodeBlock = CodeBlock.builder()
      val releaseArgumentsCodeBlock = CodeBlock.builder()
      val activityMethodElement = elementForName(ACTIVITY_METHOD_CLASS_NAME).asType().asElement().asTypeElement()
      val callbackRawClassName = ClassName.get(activityMethodElement)
      val callbackTypeName = ParameterizedTypeName.get(callbackRawClassName, returnTypeName.box())

      val callbackAnonymousClassInit = TypeSpec.anonymousClassBuilder("")
            .superclass(callbackTypeName)
            .apply {
               //Generate a field for captured arguments and the corresponding capture / release
               methodToOverride.parameters.forEachIndexed { i, argument ->
                  val fieldType = ClassName.get(argument.asType())
                  val field = FieldSpec.builder(fieldType.box(), argument.simpleName.toString())
                        .addModifiers(Modifier.PRIVATE)
                        .build()

                  addField(field)

                  captureArgumentsCodeBlock.addStatement(
                        "this.\$N = (\$T)(args[\$L])",
                        field,
                        fieldType,
                        i
                  )

                  releaseArgumentsCodeBlock.addStatement(
                        "this.\$N = null",
                        field
                  )
               }
            }
            //Implement capture argument methods if there is anything to capture
            .apply {
               if (methodToOverride.parameters.isNotEmpty()) {
                  addMethod(MethodSpec.overriding(callbackClassElement.findMethodByName("captureArguments"))
                        .addCode(captureArgumentsCodeBlock.build())
                        .build())
                  addMethod(MethodSpec.overriding(callbackClassElement.findMethodByName("releaseArguments"))
                        .addCode(releaseArgumentsCodeBlock.build())
                        .build())
               }
            }
            //Add activity method call
            .addMethod(MethodSpec.methodBuilder("callActivityMethod")
                  .returns(returnTypeName.box())
                  .addAnnotation(Override::class.java)
                  .addModifiers(Modifier.PUBLIC)
                  .apply {
                     val invocationStatement = methodToOverride.toInvocationString()
                     if (isVoidMethod) {
                        addStatement("\$L", invocationStatement)
                        addStatement("return null")
                     } else {
                        addStatement("return \$L", invocationStatement)
                     }
                  }
                  .build()
            ).build()

      val callbackMethodField = FieldSpec.builder(callbackTypeName, methodToOverride.simpleName.toString())
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .initializer(CodeBlock.of("\$L", callbackAnonymousClassInit))
            .build()

      return callbackMethodField
   }
}

