package com.bq.autoactivity.compiler.models

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
            .filter { !it.simpleName.matches("(get|set)[A-Z].*".toRegex()) } //Ignore properties

      plugins.forEach { p ->
         p.callbackMethods.forEach { callback ->
            val match = activityMethods.firstOrNull { callback.matchesActivityMethod(it) }
            if (match == null) {
               logWarning("Not matching method found for $callback", callback.callbackMethod)
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
      val activityMethodElement = elementForName(ACTIVITY_METHOD_CLASS_NAME).asType().asElement().asTypeElement()
      val callbackRawClassName = ClassName.get(activityMethodElement)

      callbackMap.forEach { methodToOverride, pluginCallbacksToInvoke ->

         val captureArgumentsCodeBlock = CodeBlock.builder()
         val releaseArgumentsCodeBlock = CodeBlock.builder()

         val callbackClassElement = elementForName(ACTIVITY_METHOD_CLASS_NAME)
         val returnTypeName = TypeName.get(methodToOverride.returnType)
         val callbackTypeName = ParameterizedTypeName.get(callbackRawClassName, returnTypeName.box())

         val isVoidMethod = methodToOverride.returnType.kind == TypeKind.VOID

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

         componentTypeSpec.addField(callbackMethodField)

         componentTypeSpec.addMethod(
               MethodSpec.overriding(methodToOverride)
                     .addStatement("this.\$N.captureArguments(\$L)", callbackMethodField,
                           methodToOverride.parameters.map { it.simpleName }.joinToString(", "))
                     .addCode("//Begin call\n")
                     .apply {
                        pluginCallbacksToInvoke.forEach {
                           if (it.canOverrideActivityMethod)
                              addStatement("this.\$L.\$L(\$L)", it.plugin.fieldName,
                                    methodToOverride.simpleName,
                                    (listOf(callbackMethodField.name).plus(methodToOverride.parameters.map { it.simpleName }))
                                          .joinToString(", "))
                           else
                              addStatement("this.\$L.\$L", it.plugin.fieldName, methodToOverride.toInvocationString())
                        }
                     }
                     .addCode("//End call\n")
                     .apply {
                        if (isVoidMethod) {
                           addStatement("if(!this.\$N.overridden()) super.\$L ", callbackMethodField, methodToOverride.toInvocationString())
                        } else {
                           addStatement("\$T returnedValue", returnTypeName)
                           beginControlFlow("if(this.\$N.overridden())", callbackMethodField)
                           addStatement("returnedValue = \$N.getOverriddenValue()", callbackMethodField)
                           endControlFlow()
                           beginControlFlow("else")
                           addStatement("returnedValue = super.\$L", methodToOverride.toInvocationString())
                           endControlFlow()
                        }
                     }
                     .addStatement("this.\$N.releaseArguments()", callbackMethodField)
                     .addStatement("this.\$N.reset()", callbackMethodField)
                     .apply {
                        if (!isVoidMethod) addStatement("return returnedValue")
                     }
                     .build()
         )
      }

      val file = JavaFile.builder(activityClassName.packageName(), componentTypeSpec.build()).build()
      file.writeTo(env.filer)
   }
}

