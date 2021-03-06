/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.testing.execution.control.refork;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;

/**
 * @author Tom Eyckmans
 */
public class ReforkDecisionContextImpl implements ReforkDecisionContext {
    private List<ReforkReasonKey> itemKeys;
    private Map<ReforkReasonKey, Object> itemData;

    public ReforkDecisionContextImpl() {
        itemKeys = new ArrayList<ReforkReasonKey>();
        itemData = new HashMap<ReforkReasonKey, Object>();
    }

    public List<ReforkReasonKey> getItemKeys() {
        return Collections.unmodifiableList(itemKeys);
    }

    public Map<ReforkReasonKey, Object> getItemData() {
        return Collections.unmodifiableMap(itemData);
    }

    public void addItem(ReforkReasonKey itemKey, Object itemData) {
        if (!itemKeys.contains(itemKey)) {
            itemKeys.add(itemKey);
        }
        if (itemData != null) {
            this.itemData.put(itemKey, itemData);
        }
    }

    public Object getData(ReforkReasonKey itemKey) {
        return itemData.get(itemKey);
    }

    public boolean isEmpty() {
        return itemData.isEmpty();
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(itemKeys);
        out.writeObject(itemData);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        itemKeys = (List<ReforkReasonKey>) in.readObject();
        itemData = (Map<ReforkReasonKey, Object>) in.readObject();
    }
}
