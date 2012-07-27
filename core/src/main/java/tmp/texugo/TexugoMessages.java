/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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

package tmp.texugo;

import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;

/**
 * @author Stuart Douglas
 */
@MessageBundle(projectCode = "TEXUGO")
public interface TexugoMessages {

    TexugoMessages MESSAGES = Messages.getBundle(TexugoMessages.class);

    @Message(id = 2, value = "The response has already been started")
    IllegalStateException responseAlreadyStarted();

    @Message(id = 4, value = "getResponseChannel() has already been called")
    IllegalStateException responseChannelAlreadyProvided();

    @Message(id = 5, value = "getRequestChannel() has already been called")
    IllegalStateException requestChannelAlreadyProvided();

    @Message(id = 6, value = "Failed to acquire request channel")
    IllegalStateException failedToAcquireRequestChannel();

    @Message(id = 7, value = "Failed to acquire response channel")
    IllegalStateException failedToAcquireResponseChannel();

    @Message(id = 8, value = "Handler cannot be null")
    IllegalArgumentException handlerCannotBeNull();

    @Message(id = 9, value = "Path must be specified")
    IllegalArgumentException pathMustBeSpecified();

}
