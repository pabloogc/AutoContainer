package com.bq.autocontainer.compiler

import com.bq.autocontainer.Callback
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.TypeKindVisitor6

class CallbackMethodModel(val plugin: PluginModel, val callbackMethod: ExecutableElement) {

   val canOverrideContainerMethod: Boolean
   val callSuper: CallSuperType
   val overrideReturnType: TypeMirror?
   val priority: Int

   init {
      canOverrideContainerMethod = callbackMethod.parameters.firstOrNull()
            ?.asType()?.asTypeElementOrNull()
            ?.qualifiedName?.toString()
            ?.equals(PluginModel.CALLBACK_METHOD_CLASS_NAME)
            ?: false

      //If not specified call and the method won't override the container method (lifecycle methods)
      //call it after super, otherwise the callback goes first
      val declaredCallSuperStrategy = let {
         ProcessorUtils.env.elementUtils.getAllAnnotationMirrors(callbackMethod).forEach loop@{
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
      callSuper = if (callSuperUnspecified) {
         if (canOverrideContainerMethod) {
            CallSuperType.AFTER
         } else {
            CallSuperType.BEFORE
         }
      } else {
         declaredCallSuperStrategy
      }

      if (canOverrideContainerMethod) {
         overrideReturnType = callbackMethod.parameters.first().asType().accept(object : TypeKindVisitor6<TypeMirror, Void>() {
            override fun visitDeclared(t: DeclaredType, p: Void?): TypeMirror? {
               return t.typeArguments[0]
            }
         }, null)
      } else {
         overrideReturnType = null
      }

      //Assign the callback priority, adjusted by relative priority and base priority
      val callbackAnnotation = callbackMethod.getAnnotation(Callback::class.java)
      val specificPriority = callbackAnnotation.priority
      priority = callbackAnnotation.relativePriority +
            if (specificPriority != Integer.MIN_VALUE) specificPriority else plugin.priority
   }

   fun matchesContainerMethod(containerMethod: ExecutableElement): Boolean {

      val parametersToMatch = callbackMethod.parameters
            .drop(if (canOverrideContainerMethod) 1 else 0) //Drop first if overriding

      val nameMatch = callbackMethod.simpleName == containerMethod.simpleName

      val parametersTypesMatch = parametersToMatch
            .zip(containerMethod.parameters)
            .map { it.first.asType().to(it.second.asType()) }
            .all { it.first.isSameType(it.second) }

      val parametersSizeMatch = parametersToMatch.size == containerMethod.parameters.size

      val returnTypeMatch = if (canOverrideContainerMethod) {
         overrideReturnType!!.implements(containerMethod.returnType)
               || containerMethod.returnType.kind == TypeKind.VOID && overrideReturnType.implements(elementForName("java.lang.Void").asType())
      } else {
         // We don't care about return type in callback methods since its discarded
         // and there is no way to mutate the caller other than the arguments
         true
      }

      return nameMatch && returnTypeMatch && parametersTypesMatch && parametersSizeMatch
   }

   override fun toString(): String {
      return "CallbackMethod(callbackMethod=$callbackMethod, canOverrideContainerMethod=$canOverrideContainerMethod, callSuper=$callSuper, returnType=$overrideReturnType)"
   }
}