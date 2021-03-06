/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.module.extension.internal.loader.java.type.AnnotationValueFetcher;
import org.mule.runtime.module.extension.internal.loader.java.type.FieldElement;
import org.mule.runtime.module.extension.internal.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.type.TypeGeneric;
import org.mule.runtime.module.extension.internal.loader.java.type.ast.ASTType;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.core.ResolvableType;

/**
 * Wrapper for {@link Class} that provide utility methods to facilitate the introspection of a {@link Class}
 *
 * @since 4.0
 */
public class TypeWrapper implements Type {

  private final java.lang.reflect.Type type;
  final Class<?> aClass;
  private List<TypeGeneric> generics = emptyList();
  private ResolvableType[] resolvableTypeGenerics;
  ClassTypeLoader typeLoader;

  public TypeWrapper(Class<?> aClass, ClassTypeLoader typeLoader) {
    this.aClass = aClass;
    this.type = aClass;
    this.typeLoader = typeLoader;
  }

  public TypeWrapper(ResolvableType resolvableType, ClassTypeLoader typeLoader) {
    this.aClass = resolvableType.getRawClass();
    this.type = resolvableType.getType();
    this.generics = new ArrayList<>();
    this.typeLoader = typeLoader;
    resolvableTypeGenerics = resolvableType.getGenerics();
    for (ResolvableType type : resolvableTypeGenerics) {
      TypeWrapper concreteType = new TypeWrapper(type, typeLoader);
      generics.add(new TypeGeneric(concreteType, concreteType.getGenerics()));
    }
  }

  public TypeWrapper(java.lang.reflect.Type type, ClassTypeLoader typeLoader) {
    this(ResolvableType.forType(type), typeLoader);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return aClass.getSimpleName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationClass) {
    return ofNullable(aClass.getAnnotation(annotationClass));
  }

  @Override
  public <A extends Annotation> Optional<AnnotationValueFetcher<A>> getValueFromAnnotation(Class<A> annotationClass) {
    return isAnnotatedWith(annotationClass)
        ? Optional.of(new ClassBasedAnnotationValueFetcher<>(annotationClass, aClass, typeLoader))
        : empty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<FieldElement> getFields() {
    return IntrospectionUtils.getFields(aClass).stream().map((Field field) -> new FieldWrapper(field, typeLoader))
        .collect(toList());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<FieldElement> getAnnotatedFields(Class<? extends Annotation>... annotations) {
    return getFields().stream().filter(field -> of(annotations).anyMatch(field::isAnnotatedWith)).collect(toList());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<Class<?>> getDeclaringClass() {
    return Optional.ofNullable(aClass);
  }


  @Override
  public boolean isAssignableFrom(Class<?> clazz) {
    return aClass != null && aClass.isAssignableFrom(clazz);
  }

  @Override
  public boolean isAssignableFrom(Type type) {
    if (type instanceof TypeWrapper) {
      return type.getDeclaringClass().get().isAssignableFrom(aClass);
    } else if (type instanceof ASTType) {
      return type.isAssignableTo(this);
    }
    return false;
  }

  @Override
  public boolean isAssignableTo(Class<?> clazz) {
    return aClass != null && clazz.isAssignableFrom(aClass);
  }

  @Override
  public boolean isAssignableTo(Type type) {
    return type.isAssignableFrom(this);
  }

  @Override
  public boolean isSameType(Type type) {
    if (type instanceof TypeWrapper) {
      Class<?> aClass = ((TypeWrapper) type).aClass;
      return type.isSameType(aClass);
    }
    return false;
  }

  @Override
  public boolean isSameType(Class<?> clazz) {
    return aClass.equals(clazz);
  }

  @Override
  public boolean isInstantiable() {
    return IntrospectionUtils.isInstantiable(aClass);
  }

  @Override
  public String getTypeName() {
    return aClass.getTypeName();
  }

  @Override
  public ClassInformationAnnotation getClassInformation() {
    return new ClassInformationAnnotation(aClass, of(resolvableTypeGenerics)
        .map(ResolvableType::getType)
        .collect(toList()));
  }

  @Override
  public boolean isAnyType() {
    if (type instanceof TypeVariable) {
      java.lang.reflect.Type[] bounds = ((TypeVariable) type).getBounds();
      if (bounds.length > 0) {
        return bounds[0].equals(Object.class);
      }
    }

    return type instanceof WildcardType && type.getTypeName().equals("?");
  }

  @Override
  public List<TypeGeneric> getGenerics() {
    return generics;
  }

  @Override
  public MetadataType asMetadataType() {
    return typeLoader.load(type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<org.mule.runtime.module.extension.internal.loader.java.type.Type> getInterfaceGenerics(Class interfaceClass) {
    return IntrospectionUtils.getInterfaceGenerics(type, interfaceClass)
        .stream()
        .map(e -> new TypeWrapper(e, typeLoader))
        .collect(toList());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;

    if (o == null || getClass() != o.getClass())
      return false;

    TypeWrapper that = (TypeWrapper) o;

    return new EqualsBuilder()
        .append(type, that.type)
        .append(aClass, that.aClass)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(type)
        .append(aClass)
        .toHashCode();
  }
}
