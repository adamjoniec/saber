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

package com.jug6ernaut.saber;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Annotation for fields which indicate that it should be looked up in the activity intent's extras
 * or fragment arguments.
 * The extra will automatically be cast to the field type. If no key is provided, the variable name
 * will be used.
 * <pre><code>
 * {@literal @}Preference("key") String title;
 * {@literal @}Preference String content; // "content" is the key for the extra
 * </code></pre>
 */
@Retention(CLASS) @Target({TYPE,FIELD}) public @interface Preference {
  String key() default "";
  String defaultValue() default "";
  String file() default "";
}
