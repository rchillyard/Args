package com.phasmidsoftware.args;

import org.junit.Test;
import scala.collection.immutable.List;
import scala.collection.immutable.Seq;
import scala.jdk.CollectionConverters;
import scala.util.Try;

import java.util.Iterator;

import static org.junit.Assert.*;

public class ArgsTest {

    @Test
    public void testParseSimple() {
        final String[] cmdLineArgs = new String[]{"1", "2"};
        final Try<Args<String>> target = Args.parseSimple(cmdLineArgs);
        assertTrue(target.isSuccess());
        assertEquals(2, target.get().size());
    }

    @Test
    public void testMake() {
        final String[] cmdLineArgs = new String[]{"1", "2"};
        final Args<String> target = Args.make(cmdLineArgs);
        assertEquals(2, target.size());
    }

    @Test
    public void testOperands() {
        final String[] cndLineArgs = new String[]{"1", "2"};
        final Args<String> target = Args.parseSimple(cndLineArgs).get();
        final Seq<String> operands = target.operands();
        final Iterable<String> iterable = CollectionConverters.IterableHasAsJava(operands).asJava();
        for (String s : iterable) System.out.println(s);
        final Iterator<String> iterator = iterable.iterator();
        assertTrue(iterator.hasNext());
        assertEquals("1", iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals("2", iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testSingleton() {
        final Arg<String> one = Arg.apply("1");
        final Args<String> target = Args.singleton(one);
        assertEquals(1, target.size());
        final List<Arg<String>> argList = target.toList();
        assertEquals(1, argList.size());
    }
}