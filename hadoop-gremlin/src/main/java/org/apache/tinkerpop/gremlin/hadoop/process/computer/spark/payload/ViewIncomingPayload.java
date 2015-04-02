/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.apache.tinkerpop.gremlin.hadoop.process.computer.spark.payload;

import org.apache.tinkerpop.gremlin.process.computer.MessageCombiner;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedVertexProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class ViewIncomingPayload<M> implements Payload {

    private List<DetachedVertexProperty<Object>> view = null;
    private final List<M> incomingMessages;

    public ViewIncomingPayload(final MessageCombiner<M> messageCombiner) {
        this.incomingMessages = null == messageCombiner ? new ArrayList<>() : new ArrayList<>(1);
    }

    public ViewIncomingPayload()  {
        this.incomingMessages = null;
    }

    public List<DetachedVertexProperty<Object>> getView() {
        return null == this.view ? Collections.emptyList() : this.view;
    }

    public void setView(final List<DetachedVertexProperty<Object>> view) {
        this.view = view;
    }

    public List<M> getIncomingMessages() {
        return null == this.incomingMessages ? Collections.emptyList() : this.incomingMessages;
    }

    public void addIncomingMessage(final M message, final MessageCombiner<M> messageCombiner) {
        if (this.incomingMessages.isEmpty() || null == messageCombiner)
            this.incomingMessages.add(message);
        else
            this.incomingMessages.set(0, messageCombiner.combine(this.incomingMessages.get(0), message));
    }
}
