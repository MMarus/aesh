/*
 * Copyright 2015 Julien Viet
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

package org.jboss.aesh.readline;

import org.jboss.aesh.terminal.Key;

import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class EventQueue implements Iterator<KeyEvent> {

  private KeyEvent[] bindings;
  private final LinkedList<KeyEvent> events = new LinkedList<>();
  private int[] pending = new int[0];

  public EventQueue() {
    this.bindings = Key.values();
  }

  public EventQueue(KeyMapper keymap) {
    this.bindings = keymap.getEvents().toArray(new KeyEvent[keymap.getEvents().size()]);
  }

  public EventQueue append(int... codePoints) {
    pending = Arrays.copyOf(pending, pending.length + codePoints.length);
    System.arraycopy(codePoints, 0, pending, pending.length - codePoints.length, codePoints.length);
    return this;
  }

  public EventQueue append(KeyEvent event) {
    events.add(event);
    return this;
  }

  public KeyEvent peek() {
    if (events.isEmpty()) {
      return match(pending);
    } else {
      return events.peekFirst();
    }
  }

  public boolean hasNext() {
    return peek() != null;
  }

  public KeyEvent next() {
    if (events.isEmpty()) {
      KeyEvent next = match(pending);
      if (next != null) {
        events.add(next);
        pending = Arrays.copyOfRange(pending, next.length(), pending.length);
      }
    }
    return events.removeFirst();
  }

  public int[] clear() {
    events.clear();
    int[] buffer = pending;
    pending = new int[0];
    return buffer;
  }

  /**
   * @return the buffer chars as a read-only int buffer
   */
  public IntBuffer getBuffer() {
    return IntBuffer.wrap(pending).asReadOnlyBuffer();
  }

  private KeyEvent match(int[] buffer) {
    if (buffer.length > 0) {
      KeyEvent candidate = null;
      int prefixes = 0;
      next:
      for (KeyEvent action : bindings) {
        if (action.length() > 0) {
          if (action.length() <= buffer.length) {
            for (int i = 0;i < action.length();i++) {
              if (action.getCodePointAt(i) != buffer[i]) {
                continue next;
              }
            }
            if (candidate != null && candidate.length() > action.length()) {
              continue next;
            }
            candidate = action;
          } else {
            for (int i = 0;i < buffer.length;i++) {
              if (action.getCodePointAt(i) != buffer[i]) {
                continue next;
              }
            }
            prefixes++;
          }
        }
      }
      if (candidate == null) {
        if (prefixes == 0) {
          final int c = buffer[0];
          return new KeyEvent() {
            @Override
            public int getCodePointAt(int index) throws IndexOutOfBoundsException {
              if (index != 0) {
                throw new IndexOutOfBoundsException("Wrong index " + index);
              }
              return c;
            }
            @Override
            public int length() {
              return 1;
            }
            @Override
            public String toString() {
              return "key:" + c;
            }
          };
        }
      } else {
        return candidate;
      }
    }
    return null;
  }
}
