package com.bq.autoactivity.compiler

import com.bq.autoactivity.compiler.AutoActivityProcessor.env
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName
import java.util.*
import javax.lang.model.element.*
import javax.lang.model.type.*
import javax.lang.model.util.ElementKindVisitor6
import javax.lang.model.util.SimpleElementVisitor6
import javax.tools.Diagnostic
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

//###################
// Conversions
//###################

fun elementForName(name: String): TypeElement = env.elementUtils.getTypeElement(name)
fun <T : Any> KClass<T>.asElement(): Element = env.elementUtils.getTypeElement(this.java.toString())
fun TypeMirror.asElement(): Element = env.typeUtils.asElement(this)
fun <T : Any> KClass<T>.asTypeMirror(): TypeMirror = this.asElement().asType()

fun Element.asTypeElement(): TypeElement = asTypeElementOrNull()!!
fun TypeMirror.asTypeElement(): TypeElement = asElement().asTypeElement()
fun Element.asTypeElementOrNull(): TypeElement? = if (this is TypeElement) this else null

fun VariableElement.asTypeElement(): TypeElement = asTypeElementOrNull()!!
fun VariableElement.asTypeElementOrNull(): TypeElement? {
  val visitor = object : ElementKindVisitor6<TypeElement, Void>() {
    override fun visitType(e: TypeElement, p: Void): TypeElement? {
      logError(e.toString())
      return e;
    }

    override fun visitTypeAsClass(e: TypeElement?, p: Void?): TypeElement? {
      return e;
    }
  }
  return this.accept(visitor, null)
}

//###################
// Type Utilities
//###################

fun TypeMirror.implements(base: KClass<*>): Boolean {
  return this.implements(base.asTypeMirror())
}

fun TypeMirror.implements(base: TypeMirror): Boolean {
  return env.typeUtils.isAssignable(this, base)
}

fun ExecutableElement.sameMethodSignature(other: ExecutableElement): Boolean {
  val m1 = this.asType() as ExecutableType
  val m2 = other.asType() as ExecutableType
  return this.simpleName == other.simpleName
      && env.typeUtils.isSubsignature(m1, m2)
      && env.typeUtils.isSubsignature(m2, m1)
}

fun TypeElement.enclosedAndInheritedElements(): List<Element> {
  val out = ArrayList<Element>()
  var current: TypeElement? = this

  while (current != null && current.asType().kind != TypeKind.NONE) {
    out.addAll(current.enclosedElements)
    val elem = env.typeUtils.asElement(current.superclass) ?: break
    current = elem as TypeElement
  }
  return out.distinct()
}

//###################
// Annotation Utilities
//###################

fun <T> T.typeMirrors(property: KProperty1<T, Array<KClass<*>>>): List<TypeMirror> {
  try {
    property.get(this)
  } catch(ex: MirroredTypesException) {
    return ex.typeMirrors
  } catch (ex: MirroredTypeException) {
    return listOf(ex.typeMirror)
  }
  throw IllegalArgumentException("Property is not a Class<?>[]")
}

fun <T> T.typeMirror(property: KProperty1<T, KClass<*>>): TypeMirror {
  try {
    property.get(this)
  } catch(ex: MirroredTypeException) {
    return ex.typeMirror
  }
  throw IllegalArgumentException("Property is not a Class<?>")
}

fun <T : Annotation> Element.hasAnnotation(type: Class<T>): Boolean = this.getAnnotation(type) != null

fun TypeMirror.isSameType(other: TypeMirror) = env.typeUtils.isSameType(this, other)

//###################
// JavaPoet utilities
//###################

fun logError(message: String, element: Element? = null) {
  logMessage(Diagnostic.Kind.ERROR, message, element)
}

fun logWarning(message: String, element: Element? = null) {
  logMessage(Diagnostic.Kind.MANDATORY_WARNING, message, element)
}


fun logMessage(kind: Diagnostic.Kind, message: String, element: Element? = null) {
  env.messager.printMessage(kind, message, element)
}

//###################
// JavaPoet utilities
//###################

fun Element.copyAnnotations(): Array<AnnotationSpec> {
  return annotationMirrors.map {
    AnnotationSpec.get(it)
  }.toTypedArray()
}

/**
 * Transform a method foo(Bar bar, Bar2 bar2) into foo(bar, bar2)
 */
fun ExecutableElement.toInvocationString(): String {
  return MethodSpec.overriding(this).build().toInvocationString()
}

fun MethodSpec.toInvocationString(): String {
  return "${this.name}(${this.parameters.map { it.name }.joinToString(", ")})"
}

fun TypeMirror.toTypeName(): TypeName = ClassName.get(this)
fun TypeMirror.toClassName(): ClassName = ClassName.get(this) as ClassName


