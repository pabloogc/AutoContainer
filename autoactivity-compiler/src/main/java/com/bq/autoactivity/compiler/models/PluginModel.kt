package com.bq.autoactivity.compiler.models

import com.bq.autoactivity.Callback
import com.bq.autoactivity.compiler.*
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.TypeSpec
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.*
import javax.lang.model.util.TypeKindVisitor6

const val ACTIVITY_METHOD_CLASS_NAME = "com.bq.autoactivity.ActivityMethod"

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

   override fun toString(): String {
      return "${className.toString()}"
   }

   inner class CallbackMethod(val callbackMethod: ExecutableElement) {

      val canOverrideActivityMethod: Boolean
      val returnType: TypeMirror?
      val plugin = this@PluginModel

      init {
         canOverrideActivityMethod = callbackMethod.parameters.firstOrNull()
               ?.asType()?.asTypeElementOrNull()
               ?.qualifiedName?.toString()
               ?.equals(ACTIVITY_METHOD_CLASS_NAME)
               ?: false

         if (canOverrideActivityMethod) {
            returnType = callbackMethod.parameters.first().asType().accept(object : TypeKindVisitor6<TypeMirror, Void>() {
               override fun visitDeclared(t: DeclaredType, p: Void?): TypeMirror? {
                  return t.typeArguments[0]
               }
            }, null)
         } else {
            returnType = null
         }
      }

      fun matchesActivityMethod(activityMethod: ExecutableElement): Boolean {
         if (canOverrideActivityMethod) {
            //Same name
            val nameMatch = callbackMethod.simpleName == activityMethod.simpleName

            //Same return type or void
            val returnTypeMatch = returnType!!.isSameType(activityMethod.returnType)
                  || activityMethod.returnType.kind == TypeKind.VOID
                  && returnType.isSameType(elementForName("java.lang.Void").asType())

            //Same parameter types
            val typesMatch = callbackMethod.parameters.drop(1).zip(activityMethod.parameters)
                  .map { it.first.asType().to(it.second.asType()) }
                  .all { it.first.isSameType(it.second) }

            //Same number of parameters
            val sizesMatch = callbackMethod.parameters.size - 1 == activityMethod.parameters.size
            return nameMatch && returnTypeMatch && typesMatch && sizesMatch
         } else {

            //Simply check both methods have the same signature
            return callbackMethod.sameMethodSignature(activityMethod)
         }
      }

   }
}