package com.link.codegen.processor.dto;

import com.google.auto.service.AutoService;
import com.link.codegen.processor.BaseCodeGenProcessor;
import com.link.codegen.spi.CodeGenProcessor;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.lang.annotation.Annotation;
import java.util.Objects;
import java.util.Set;

/**
 * @author gim dto 代码生成器
 */
@AutoService(value = CodeGenProcessor.class)
public class DtoCodeGenProcessor extends BaseCodeGenProcessor {

  public static final String SUFFIX = "DTO";

  @Override
  public Class<? extends Annotation> getAnnotation() {
    return GenDto.class;
  }

  @Override
  public String generatePackage(TypeElement typeElement) {
    return typeElement.getAnnotation(GenDto.class).pkgName();
  }

  @Override
  protected void generateClass(TypeElement typeElement, RoundEnvironment roundEnvironment) {
    Set<VariableElement> fields = findFields(typeElement,
        ve -> Objects.isNull(ve.getAnnotation(IgnoreDto.class)));
    String className = PREFIX + typeElement.getSimpleName() + SUFFIX; //CustomerDto
//    String className = PREFIX + typeElement.getSimpleName() + SUFFIX;
//    String sourceClassName = typeElement.getSimpleName() + SUFFIX;
    Builder builder = TypeSpec.classBuilder(className)
//        .superclass(AbstractBaseJpaVO.class)
// 不需要父类
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Schema.class)
        .addAnnotation(Data.class);
    addSetterAndGetterMethod(builder, fields);
//    MethodSpec.Builder constructorSpecBuilder = MethodSpec.constructorBuilder()
//        .addParameter(TypeName.get(typeElement.asType()), "source")
//        .addModifiers(Modifier.PUBLIC);
//    constructorSpecBuilder.addStatement("super(source)");
//    fields.stream().forEach(f -> {
//      constructorSpecBuilder.addStatement("this.set$L(source.get$L())", getFieldDefaultName(f),
//          getFieldDefaultName(f));
//    });
    // $L  占位符
    builder.addMethod(MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PROTECTED)
        .build());
//    builder.addMethod(constructorSpecBuilder.build());
    String packageName = generatePackage(typeElement);
//    genJavaFile(packageName, builder);
//    genJavaFile(packageName, getSourceTypeWithConstruct(typeElement,sourceClassName, packageName, className));


//    生成源文件
    genJavaSourceFile(generatePackage(typeElement),
            typeElement.getAnnotation(GenDto.class).sourcePath(), builder);
  }
}
