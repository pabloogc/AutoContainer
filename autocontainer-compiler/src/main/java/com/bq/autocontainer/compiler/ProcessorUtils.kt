package com.bq.autocontainer.compiler

import com.bq.autocontainer.compiler.ProcessorUtils.env
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName
import java.util.*
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.*
import javax.lang.model.type.*
import javax.lang.model.util.ElementKindVisitor6
import javax.tools.Diagnostic
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1


object ProcessorUtils {
   lateinit var env: ProcessingEnvironment
}


//####################
// Conversions
//####################

fun elementForName(name: String): TypeElement = env.elementUtils.getTypeElement(name)

fun <T : Any> KClass<T>.asElement(): Element = env.elementUtils.getTypeElement(this.java.toString())
fun <T : Any> KClass<T>.asTypeMirror(): TypeMirror = this.asElement().asType()

fun TypeMirror.asElement(): Element = asElementOrNull()!!
fun TypeMirror.asElementOrNull(): Element? = env.typeUtils.asElement(this)


fun Element.asTypeElement(): TypeElement = asTypeElementOrNull()!!
fun Element.asTypeElementOrNull(): TypeElement? = if (this is TypeElement) this else null

fun TypeMirror.asTypeElement(): TypeElement = asTypeElementOrNull()!!
fun TypeMirror.asTypeElementOrNull(): TypeElement? = asElementOrNull()?.asTypeElementOrNull()


fun VariableElement.asTypeElement(): TypeElement = asTypeElementOrNull()!!
fun VariableElement.asTypeElementOrNull(): TypeElement? {
   val visitor = object : ElementKindVisitor6<TypeElement, Void>() {
      override fun visitType(e: TypeElement, p: Void): TypeElement? {
         logError(e.toString())
         return e
      }

      override fun visitTypeAsClass(e: TypeElement?, p: Void?): TypeElement? {
         return e
      }
   }
   return this.accept(visitor, null)
}

//####################
// Type Utilities
//####################

fun Element.findMethodByName(name: String): ExecutableElement = findMethodByNameOrNull(name)!!
fun Element.findMethodByNameOrNull(name: String): ExecutableElement? = findMethodOrNull { it.simpleName.toString() == name }
fun Element.findMethod(p: (ExecutableElement) -> Boolean): ExecutableElement = findMethodOrNull(p)!!
fun Element.findMethodOrNull(p: (ExecutableElement) -> Boolean): ExecutableElement? {
   return this.enclosedElements.filter { it.kind == ElementKind.METHOD }
         .map { it as ExecutableElement }
         .firstOrNull(p)
}

val Element.isMethod: Boolean get() = this.kind == javax.lang.model.element.ElementKind.METHOD
val Element.isAbstract: Boolean get() = this.modifiers.contains(Modifier.ABSTRACT)
val ExecutableElement.isVoid: Boolean get() = this.returnType.kind == TypeKind.VOID

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

/**
 * All enclosed elements, excluding Object members (wait, notify...).
 */
val TypeElement.allUserEnclosedElements: List<Element>
   get() {
      val out = ArrayList<Element>()
      var current: TypeElement? = this

      while (current != null && current.asType().kind != TypeKind.NONE) {
         out.addAll(current.enclosedElements)
         val elem = env.typeUtils.asElement(current.superclass) ?: break
         current = elem as TypeElement
      }
      return out.distinct()
   }

val TypeElement.allEnclosedElements: List<Element> get() = env.elementUtils.getAllMembers(this)

//####################
// Annotation Utilities
//####################

fun <T : Annotation> T.typeMirrors(property: KProperty1<T, Array<KClass<*>>>): List<TypeMirror> {
   try {
      property.get(this)
   } catch(ex: MirroredTypesException) {
      return ex.typeMirrors
   }
   throw IllegalArgumentException("Property is not a Class<?>[]")
}

fun <T : Annotation> T.typeMirror(property: KProperty1<T, KClass<*>>): TypeMirror {
   try {
      property.get(this)
   } catch(ex: MirroredTypeException) {
      return ex.typeMirror
   }
   throw IllegalArgumentException("Property is not a Class<?>")
}

fun Annotation.typeMirror(access: () -> Class<*>): TypeMirror {
   try {
      access()
   } catch(ex: MirroredTypeException) {
      return ex.typeMirror
   }
   throw IllegalArgumentException("Property is not a Class<?>")
}

fun Annotation.typeMirrors(access: () -> Array<Class<*>>): List<TypeMirror> {
   try {
      access()
   } catch(ex: MirroredTypesException) {
      return ex.typeMirrors
   }
   throw IllegalArgumentException("Property is not a Class<?>[]")
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

//####################
// JavaPoet utilities
//####################

fun MethodSpec.Builder.breakLine() = addCode("\n")
fun MethodSpec.Builder.addComment(format: String, vararg args: Any?) = addCode("//$format\n", args)

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


