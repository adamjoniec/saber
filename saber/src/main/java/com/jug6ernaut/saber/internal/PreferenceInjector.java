/*
 * Copyright 2013 Jake Wharton
 * Copyright 2014 Prateek Srivastava (@f2prateek)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jug6ernaut.saber.internal;

import com.google.gson.Gson;

import javax.lang.model.type.TypeMirror;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.jug6ernaut.saber.internal.InjectPreferenceProcessor.isNullOrEmpty;

final class PreferenceInjector {
  private final Map<String, PreferenceInjection> injectionMap = new LinkedHashMap<>();
  private final Map<String, String> onChangeMap = new LinkedHashMap<>(); // file/method name
  private final String classPackage;
  private final String className;
  private final String targetClass;
  private String parentInjector;
  private String fileName;

  PreferenceInjector(String classPackage, String className, String targetClass) {
    this.classPackage = classPackage;
    this.className = className;
    this.targetClass = targetClass;
  }

  static void emitCast(StringBuilder builder, TypeMirror fieldType) {
    builder.append('(').append(getType(fieldType)).append(") ");
  }

  static String getType(TypeMirror type) {
    if (type.getKind().isPrimitive()) {
      // Get wrapper for primitive types
      switch (type.getKind()) {
        case BOOLEAN:
          return "java.lang.Boolean";
        case BYTE:
          return "java.lang.Byte";
        case SHORT:
          return "java.lang.Short";
        case INT:
          return "java.lang.Integer";
        case LONG:
          return "java.lang.Long";
        case CHAR:
          return "java.lang.Character";
        case FLOAT:
          return "java.lang.Float";
        case DOUBLE:
          return "java.lang.Double";
        default:
          // Shouldn't happen
          throw new RuntimeException();
      }
    } else {
      return type.toString();
    }
  }

  static void emitHumanDescription(StringBuilder builder, List<Binding> bindings) {
    switch (bindings.size()) {
      case 1:
        builder.append(bindings.get(0).getDescription());
        break;
      case 2:
        builder.append(bindings.get(0).getDescription())
            .append(" and ")
            .append(bindings.get(1).getDescription());
        break;
      default:
        for (int i = 0, count = bindings.size(); i < count; i++) {
          Binding requiredField = bindings.get(i);
          if (i != 0) {
            builder.append(", ");
          }
          if (i == count - 1) {
            builder.append("and ");
          }
          builder.append(requiredField.getDescription());
        }
        break;
    }
  }

  void addField(String name, String file, String key, String defaultValue, TypeMirror type) {
    getOrCreateExtraBinding(file,key,defaultValue,type).addFieldBinding(new FieldBinding(name, file, key, defaultValue, type));
  }

  void setParentInjector(String parentInjector) {
    this.parentInjector = parentInjector;
  }

  private PreferenceInjection getOrCreateExtraBinding(String file, String key, String defaultValue, TypeMirror type) {
    PreferenceInjection preferenceInjection = injectionMap.get(file+key);
    if (preferenceInjection == null) {
      preferenceInjection = new PreferenceInjection(file,key,defaultValue,type);
      injectionMap.put(file+key, preferenceInjection);
    }
    return preferenceInjection;
  }

  String getFqcn() {
    return classPackage + "." + className;
  }

  String brewJava() {
    StringBuilder builder = new StringBuilder();
    builder.append("// Generated code from Saber. Do not modify!\n");
    builder.append("package ").append(classPackage).append(";\n\n");
    builder.append("import com.jug6ernaut.saber.Saber.Finder;\n\n");
    builder.append("import android.content.Context;\n\n");
    builder.append("import com.jug6ernaut.saber.preferences.Preference;\n\n");
    if (!onChangeMap.isEmpty())builder.append("import android.content.SharedPreferences;\n\n");
    builder.append("public class ").append(className).append(" {\n");
    emitInject(builder);
    builder.append("}\n");
    return builder.toString();
  }


  //Context context, String file, String key, Object defaultValue, Class<Preference> type
  private void emitInject(StringBuilder builder) {
    builder.append("  public static void inject(")
        .append("final Context context, final ")
        .append(targetClass)
        .append(" target")

        .append(") {\n");

    // Emit a call to the superclass injector, if any.
//    if (parentInjector != null) {
//      builder.append("    ").append(parentInjector).append(".inject(finder, target, source);\n\n");
//    }

    // Local variable in which all extras will be temporarily stored.
    builder.append("    Preference object;\n");

    // Loop over each extras injection and emit it.
    for (PreferenceInjection injection : injectionMap.values()) {
      emitExtraInjection(builder, injection);
    }

    for (Map.Entry<String, String> entry : onChangeMap.entrySet()) {
      String targetFile = entry.getKey();
      String methodName = entry.getValue();

      emitOnChangeBinding(builder,methodName,targetFile);
    }

    builder.append("  }\n");
  }

  private void emitExtraInjection(StringBuilder builder, PreferenceInjection injection) {

    String file;
    if(!isNullOrEmpty(injection.getFile())) {
      file = injection.getFile(); // annotation level
    } else if(!isNullOrEmpty(fileName)) {
      file = fileName; // class level
    } else {
      file = targetClass; // fall back to targetClass
    }

    builder.append("    object = Finder.getPreference(context, ")
        .append("\"").append(file).append("\", ")
        .append("\"").append(injection.getKey()).append("\", ")
        .append("").append(new Gson().toJson(injection.getDefaultValue())).append(", ")
        .append("").append("(Class<Preference>) new com.google.gson.reflect.TypeToken<").append(injection.getType()).append(">(){}.getRawType()").append("")
        .append(");\n");

    List<Binding> requiredBindings = injection.getRequiredBindings();
    if (!requiredBindings.isEmpty()) {
      builder.append("    if (object == null) {\n")
          .append("      throw new IllegalStateException(\"Required extra with key '")
          .append(injection.getKey())
              .append("' for ");
      emitHumanDescription(builder, requiredBindings);
      builder.append(" was not found. If this extra is optional add '@Nullable' annotation.\");\n")
          .append("    }\n");
      emitFieldBindings(builder, injection);
    } else {
      // an optional extra, wrap it in a check to keep original value, if any
      builder.append("    if (object != null) {\n");
      builder.append("  ");
      emitFieldBindings(builder, injection);
      builder.append("    }\n");
    }
  }

  private void emitFieldBindings(StringBuilder builder, PreferenceInjection injection) {
    Collection<FieldBinding> fieldBindings = injection.getFieldBindings();
    if (fieldBindings.isEmpty()) {
      return;
    }

    for (FieldBinding fieldBinding : fieldBindings) {
      builder.append("    target.").append(fieldBinding.getName()).append(" = ");

      emitCast(builder, fieldBinding.getType());
      builder.append("object;\n");
    }
  }

  private void emitOnChangeBinding(StringBuilder builder, String methodName, String targetFile) {
    builder.append("    context.getSharedPreferences(").append("\"");
    builder.append(targetFile).append("\"").append(",").append("Context.MODE_PRIVATE") .append(")").append("\n");
    builder.append("    .registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {").append("\n");
    builder.append("      @Override").append("\n");
    builder.append("      public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {").append("\n");
    builder.append("        target.").append(methodName).append("( key );").append("\n");
    builder.append("      }").append("\n");
    builder.append("    });").append("\n");
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getClassName() {
    return className;
  }

  public String getTargetClass() {
    return targetClass;
  }

  public Map<String, String> getOnChangeMap() {
    return onChangeMap;
  }
}
