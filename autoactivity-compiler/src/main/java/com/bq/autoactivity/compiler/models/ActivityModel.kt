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
import javax.lang.model.type.TypeMirror

class ActivityModel(
    originalClassName: ClassName,
    val element: TypeElement,
    val plugins: List<PluginModel>) {

  val baseClass: TypeMirror
  val activityClassName: ClassName
  val baseClassElement: TypeElement
  val callbackMap: HashMap<ExecutableElement, MutableList<PluginModel>>
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
          logWarning("Not matching method found for $callback", callback.method)
        } else {
          callbackMap.getOrPut(match, { ArrayList() }).add(p)
        }
      }
    }
  }

  fun generateClass() {
    val componentTypeSpec = TypeSpec.classBuilder(activityClassName)
        .superclass(ClassName.get(baseClass))
        .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)

    plugins.forEach {
      componentTypeSpec.addField(FieldSpec.builder(it.type.toTypeName(), it.fieldName)
          .addAnnotation(Inject::class.java)
          .addAnnotations(it.method.copyAnnotations().asIterable())
          .build())
    }

    callbackMap.forEach { methodToOverride, pluginsToInvoke ->
      val m = MethodSpec.overriding(methodToOverride)
      pluginsToInvoke.forEach {
        m.addStatement("this.\$L.\$L", it.fieldName, m.build().toInvocationString())
      }
      componentTypeSpec.addMethod(m.build())
    }

    val file = JavaFile.builder(activityClassName.packageName(), componentTypeSpec.build()).build()
    file.writeTo(env.filer)
  }
}

