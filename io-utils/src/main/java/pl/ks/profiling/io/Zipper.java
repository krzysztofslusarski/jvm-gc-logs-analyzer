/*
 * Copyright 2020 Artur Owczarek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pl.ks.profiling.io;

import lombok.experimental.UtilityClass;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@UtilityClass
public class Zipper {
    <T, U> List<Pair<T, U>> zip(List<T> ts, List<U> us) {
        Iterator<T> t = ts.iterator();
        Iterator<U> u = us.iterator();
        List<Pair<T, U>> zipped = new ArrayList<>(Math.min(ts.size(), us.size()));
        while (t.hasNext() && u.hasNext()) {
            zipped.add(new Pair<T, U>(t.next(), u.next()));
        }
        return zipped;
    }
}
