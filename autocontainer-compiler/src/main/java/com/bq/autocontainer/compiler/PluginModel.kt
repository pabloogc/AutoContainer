package com.bq.autocontainer.compiler


import com.bq.autocontainer.Callback
import com.bq.autocontainer.Plugin
import com.squareup.javapoet.ClassName
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement


class PluginModel(
      val pluginAnnotation: Plugin,
      val declaringMethod: ExecutableElement,
      val element: TypeElement,
      val fieldNameInContainer: String) {

   companion object {
      const val CALLBACK_METHOD_CLASS_NAME = "com.bq.autocontainer.CallbackMethod"
   }

   val className: ClassName
   val callbackMethods: List<CallbackMethodModel>
   val priority: Int

   init {
      className = ClassName.get(element)
      priority = pluginAnnotation.priority

      if (element.isAbstract) {
         logError("Plugin can't be abstract", element)
      }

      if (element.enclosedElements
            .filter { it.kind == ElementKind.CONSTRUCTOR }
            .map { it as ExecutableElement }
            .firstOrNull { it.modifiers.contains(Modifier.PUBLIC) && it.parameters.isEmpty() } == null) {
         logError("Plugin must have visible empty constructor.")
      }

      callbackMethods = element.allEnclosedElements
            .filter { it.isMethod }
            .filter { it.hasAnnotation(Callback::class.java) }
            .map { CallbackMethodModel(this, it as ExecutableElement) }
   }


   override fun toString(): String {
      return "PluginModel(fieldNameInContainer='$fieldNameInContainer', className=$className, callbackMethods=${callbackMethods.toString().replace(",", "\n")})"
   }
}

