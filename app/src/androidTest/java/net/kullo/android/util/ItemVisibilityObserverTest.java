/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.util;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@Ignore("Fails on slow arm emulators; works fine on x86 emulator and armv7 hardware")
public class ItemVisibilityObserverTest {
    @Test
    public void emptyState() {
        ItemVisibilityObserver<String> uut = new ItemVisibilityObserver<>(0);
        Set<String> readyItems = uut.getReadyItems();
        assertTrue(readyItems.isEmpty());
    }

    @Test
    public void allRemoved() {
        {
            ItemVisibilityObserver<String> uut = new ItemVisibilityObserver<>(0);
            uut.add("a");
            uut.remove("a");
            Set<String> readyItems = uut.getReadyItems();
            assertTrue(readyItems.isEmpty());
        }
    }

    @Test
    public void removedNonExisting() {
        {
            ItemVisibilityObserver<String> uut = new ItemVisibilityObserver<>(0);
            uut.remove("a");
            Set<String> readyItems = uut.getReadyItems();
            assertTrue(readyItems.isEmpty());
        }
        {
            ItemVisibilityObserver<String> uut = new ItemVisibilityObserver<>(0);
            uut.add("a");
            uut.remove("a");
            uut.remove("b");
            Set<String> readyItems = uut.getReadyItems();
            assertTrue(readyItems.isEmpty());
        }
        {
            ItemVisibilityObserver<String> uut = new ItemVisibilityObserver<>(0);
            uut.add("a");
            uut.remove("a");
            uut.remove("a");
            Set<String> readyItems = uut.getReadyItems();
            assertTrue(readyItems.isEmpty());
        }
    }

    @Test
    public void findInserted() {
        {
            ItemVisibilityObserver<String> uut = new ItemVisibilityObserver<>(0);
            uut.add("a");
            Set<String> readyItems = uut.getReadyItems();
            assertEquals(readyItems.size(), 1);
            assertThat(readyItems, hasItems("a"));
        }
        {
            ItemVisibilityObserver<String> uut = new ItemVisibilityObserver<>(0);
            uut.add("a");
            uut.add("a");
            Set<String> readyItems = uut.getReadyItems();
            assertEquals(readyItems.size(), 1);
            assertThat(readyItems, hasItems("a"));
        }
        {
            ItemVisibilityObserver<String> uut = new ItemVisibilityObserver<>(0);
            uut.add("a");
            uut.add("b");
            Set<String> readyItems = uut.getReadyItems();
            assertEquals(readyItems.size(), 2);
            assertThat(readyItems, hasItems("a", "b"));
        }
    }

    @Test
    public void donNotFindTooYoung() {
        {
            ItemVisibilityObserver<String> uut = new ItemVisibilityObserver<>(100);
            uut.add("a");
            Set<String> readyItems = uut.getReadyItems();
            assertThat(readyItems.size(), is(0));
        }
        {
            ItemVisibilityObserver<String> uut = new ItemVisibilityObserver<>(100);
            uut.add("a");
            uut.add("a");
            Set<String> readyItems = uut.getReadyItems();
            assertThat(readyItems.size(), is(0));
        }
        {
            ItemVisibilityObserver<String> uut = new ItemVisibilityObserver<>(100);
            uut.add("a");
            uut.add("b");
            Set<String> readyItems = uut.getReadyItems();
            assertThat(readyItems.size(), is(0));
        }
    }

    @Test
    public void clear() {
        {
            ItemVisibilityObserver<String> uut = new ItemVisibilityObserver<>(0);
            uut.clear();
            Set<String> readyItems = uut.getReadyItems();
            assertThat(readyItems.size(), is(0));
        }
        {
            ItemVisibilityObserver<String> uut = new ItemVisibilityObserver<>(0);
            uut.add("a");
            uut.add("b");
            uut.clear();
            Set<String> readyItems = uut.getReadyItems();
            assertThat(readyItems.size(), is(0));
        }
        {
            ItemVisibilityObserver<String> uut = new ItemVisibilityObserver<>(0);
            uut.add("a");
            uut.clear();
            uut.add("b");
            uut.clear();
            Set<String> readyItems = uut.getReadyItems();
            assertThat(readyItems.size(), is(0));
        }
    }

    @Test
    public void getByTime() throws InterruptedException {
        {
            ItemVisibilityObserver<String> uut = new ItemVisibilityObserver<>(50);
            uut.add("a");
            uut.add("b");

            Thread.sleep(10);
            assertThat(uut.getReadyItems().size(), is(0));

            Thread.sleep(20);
            assertThat(uut.getReadyItems().size(), is(0));

            Thread.sleep(20);
            Set<String> readyItems = uut.getReadyItems();
            assertThat(readyItems.size(), is(2));
            assertThat(readyItems, hasItems("a", "b"));
        }
        {
            ItemVisibilityObserver<String> uut = new ItemVisibilityObserver<>(50);
            uut.add("a");
            uut.add("b");

            Thread.sleep(20);
            assertThat(uut.getReadyItems().size(), is(0));
            uut.add("c");

            Thread.sleep(30);
            Set<String> readyItems = uut.getReadyItems();
            assertThat(readyItems.size(), is(2));
            assertThat(readyItems, hasItems("a", "b"));

            Thread.sleep(20);
            Set<String> readyItems2 = uut.getReadyItems();
            assertThat(readyItems2.size(), is(3));
            assertThat(readyItems2, hasItems("a", "b", "c"));
        }
        {
            ItemVisibilityObserver<String> uut = new ItemVisibilityObserver<>(50);
            uut.add("a");
            uut.add("b");

            Thread.sleep(20);
            assertThat(uut.getReadyItems().size(), is(0));

            uut.add("c");

            Thread.sleep(30);
            Set<String> readyItems = uut.getReadyItems();
            assertThat(readyItems.size(), is(2));
            assertThat(readyItems, hasItems("a", "b"));

            uut.remove("a");
            uut.add("a");

            Thread.sleep(20);
            Set<String> readyItems2 = uut.getReadyItems();
            assertThat(readyItems2.size(), is(2));
            assertThat(readyItems2, hasItems("b", "c"));
        }
    }

    @Test
    public void resetTimes() throws InterruptedException {
        {
            ItemVisibilityObserver<String> uut = new ItemVisibilityObserver<>(20);
            uut.add("a");
            uut.add("b");

            Thread.sleep(10);
            assertThat(uut.getReadyItems().size(), is(0));

            uut.resetTimes();

            Thread.sleep(10);
            assertThat(uut.getReadyItems().size(), is(0));

            Thread.sleep(20);
            Set<String> readyItems = uut.getReadyItems();
            assertThat(readyItems.size(), is(2));
            assertThat(readyItems, hasItems("a", "b"));
        }
        {
            ItemVisibilityObserver<String> uut = new ItemVisibilityObserver<>(20);
            uut.resetTimes();
            uut.add("a");
            uut.add("b");

            Thread.sleep(25);
            Set<String> readyItems = uut.getReadyItems();
            assertThat(readyItems.size(), is(2));
            assertThat(readyItems, hasItems("a", "b"));
        }
        {
            ItemVisibilityObserver<String> uut = new ItemVisibilityObserver<>(20);
            uut.add("a");
            uut.add("b");
            uut.resetTimes();

            Thread.sleep(25);
            Set<String> readyItems = uut.getReadyItems();
            assertThat(readyItems.size(), is(2));
            assertThat(readyItems, hasItems("a", "b"));
        }
        {
            ItemVisibilityObserver<String> uut = new ItemVisibilityObserver<>(50);
            uut.add("a");
            uut.add("b");

            Thread.sleep(20);
            assertThat(uut.getReadyItems().size(), is(0));

            uut.add("c");

            Thread.sleep(30);
            Set<String> readyItems = uut.getReadyItems();
            assertThat(readyItems.size(), is(2));
            assertThat(readyItems, hasItems("a", "b"));

            uut.resetTimes();

            Thread.sleep(20);
            assertThat(uut.getReadyItems().size(), is(0));

            Thread.sleep(30);
            Set<String> readyItems2 = uut.getReadyItems();
            assertThat(readyItems2.size(), is(3));
            assertThat(readyItems2, hasItems("a", "b", "c"));
        }
    }

    @Test
    public void resetTimesExcept() throws InterruptedException {
        {
            ItemVisibilityObserver<String> uut = new ItemVisibilityObserver<>(20);
            uut.add("a");
            uut.add("b");

            Thread.sleep(10);
            assertThat(uut.getReadyItems().size(), is(0));

            uut.resetTimesExcept(Collections.<String>emptySet());

            Thread.sleep(10);
            assertThat(uut.getReadyItems().size(), is(0));

            Thread.sleep(20);
            Set<String> readyItems = uut.getReadyItems();
            assertThat(readyItems.size(), is(2));
            assertThat(readyItems, hasItems("a", "b"));
        }
        {
            ItemVisibilityObserver<String> uut = new ItemVisibilityObserver<>(20);
            uut.resetTimesExcept(Collections.<String>emptySet());
            uut.add("a");
            uut.add("b");

            Thread.sleep(25);
            Set<String> readyItems = uut.getReadyItems();
            assertThat(readyItems.size(), is(2));
            assertThat(readyItems, hasItems("a", "b"));
        }
        {
            ItemVisibilityObserver<String> uut = new ItemVisibilityObserver<>(20);
            uut.add("a");
            uut.add("b");
            uut.add("c");

            Thread.sleep(10);

            uut.resetTimesExcept(Collections.singleton("a"));

            Thread.sleep(15);
            Set<String> readyItems = uut.getReadyItems();
            assertThat(readyItems.size(), is(1));
            assertThat(readyItems, hasItems("a"));

            Thread.sleep(10);
            Set<String> readyItems2 = uut.getReadyItems();
            assertThat(readyItems2.size(), is(3));
            assertThat(readyItems2, hasItems("a", "b", "c"));
        }
        {
            ItemVisibilityObserver<String> uut = new ItemVisibilityObserver<>(20);
            uut.add("a");
            uut.add("b");
            uut.add("c");

            Thread.sleep(10);

            uut.resetTimesExcept(new HashSet<>(Arrays.asList("a", "b")));

            Thread.sleep(15);
            Set<String> readyItems = uut.getReadyItems();
            assertThat(readyItems.size(), is(2));
            assertThat(readyItems, hasItems("a", "b"));

            uut.resetTimesExcept(new HashSet<>(Arrays.asList("b", "c")));

            Thread.sleep(10);
            Set<String> readyItems2 = uut.getReadyItems();
            assertThat(readyItems2.size(), is(2));
            assertThat(readyItems2, hasItems("b", "c"));

            Thread.sleep(15);
            Set<String> readyItems3 = uut.getReadyItems();
            assertThat(readyItems3.size(), is(3));
            assertThat(readyItems3, hasItems("a", "b", "c"));
        }
    }

}
