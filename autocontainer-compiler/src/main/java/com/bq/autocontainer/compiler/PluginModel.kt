package com.bq.autocontainer.compiler


import com.bq.autocontainer.Callback
import com.bq.autocontainer.compiler.ProcessorUtils.env
import com.squareup.javapoet.ClassName
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.TypeKindVisitor6

const val CALLBACK_METHOD_CLASS_NAME = "com.bq.autocontainer.CallbackMethod"

class PluginModel(
      val declaringMethod: ExecutableElement,
      val element: TypeElement,
      val fieldName: String) {

   val className: ClassName

   val callbackMethods: List<CallbackMethod>

   init {
      className = ClassName.get(element)
      //Since methods must have unique names in the interfaces this is safe

      callbackMethods = element.enclosedAndInheritedElements()
            .filter { it.kind == ElementKind.METHOD }
            .filter { it.hasAnnotation(Callback::class.java) }
            .map { CallbackMethod(it as ExecutableElement) }
   }


   enum class CallSuperType {
      BEFORE, AFTER, UNSPECIFIED
   }

   inner class CallbackMethod(val callbackMethod: ExecutableElement) {

      val canOverrideActivityMethod: Boolean
      val callSuper: CallSuperType
      val overrideReturnType: TypeMirror?
      val plugin = this@PluginModel

      init {
         canOverrideActivityMethod = callbackMethod.parameters.firstOrNull()
               ?.asType()?.asTypeElementOrNull()
               ?.qualifiedName?.toString()
               ?.equals(CALLBACK_METHOD_CLASS_NAME)
               ?: false


         //If not specified call and the method won't override the container method (lifecycle methods)
         //call it after super, otherwise the callback goes first
         val declaredCallSuperStrategy = let {
            env.elementUtils.getAllAnnotationMirrors(callbackMethod).forEach loop@{
               it.elementValues.entries.forEach { entry ->
                  if (entry.key.simpleName.toString() == "callSuper") {
                     val enumValue = entry.value.toString().substringAfterLast(".")
                     return@let CallSuperType::class.java.enumConstants.first() { it.name == enumValue }
                  }
               }
            }
            CallSuperType.UNSPECIFIED
         }

         val callSuperUnspecified = declaredCallSuperStrategy == CallSuperType.UNSPECIFIED
         callSuper = if (callSuperUnspecified)
            if (canOverrideActivityMethod) CallSuperType.AFTER else CallSuperType.BEFORE
         else declaredCallSuperStrategy

         if (canOverrideActivityMethod) {
            overrideReturnType = callbackMethod.parameters.first().asType().accept(object : TypeKindVisitor6<TypeMirror, Void>() {
               override fun visitDeclared(t: DeclaredType, p: Void?): TypeMirror? {
                  return t.typeArguments[0]
               }
            }, null)
         } else {
            overrideReturnType = null
         }
      }

      fun matchesActivityMethod(activityMethod: ExecutableElement): Boolean {

         val parametersToMatch = callbackMethod.parameters
               .drop(if (canOverrideActivityMethod) 1 else 0) //Drop first if overriding

         val nameMatch = callbackMethod.simpleName == activityMethod.simpleName

         val parametersTypesMatch = parametersToMatch
               .zip(activityMethod.parameters)
               .map { it.first.asType().to(it.second.asType()) }
               .all { it.first.isSameType(it.second) }

         val parametersSizeMatch = parametersToMatch.size == activityMethod.parameters.size

         val returnTypeMatch = if (canOverrideActivityMethod) {
            overrideReturnType!!.implements(activityMethod.returnType)
                  || activityMethod.returnType.kind == TypeKind.VOID && overrideReturnType.implements(elementForName("java.lang.Void").asType())
         } else {
            // We don't care about return type in callback methods since its discarded
            // and there is no way to mutate the caller other than the arguments
            true
         }

         return nameMatch && returnTypeMatch && parametersTypesMatch && parametersSizeMatch
      }

      override fun toString(): String {
         return "CallbackMethod(callbackMethod=$callbackMethod, canOverrideActivityMethod=$canOverrideActivityMethod, callSuper=$callSuper, returnType=$overrideReturnType)"
      }
   }

   override fun toString(): String {
      return "PluginModel(fieldName='$fieldName', className=$className, callbackMethods=${callbackMethods.toString().replace(",", "\n")})"
   }
}

