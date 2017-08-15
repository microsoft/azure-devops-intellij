// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.models;

import com.microsoft.alm.plugin.AbstractTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

public class VersionSpecTest extends AbstractTest {
    @Test
    public void testCreate_int() {
        VersionSpec spec1 = VersionSpec.create(1);
        Assert.assertEquals(VersionSpec.Type.Changeset, spec1.getType());
        Assert.assertEquals("1", spec1.getValue());

        VersionSpec spec2 = VersionSpec.create(10001);
        Assert.assertEquals(VersionSpec.Type.Changeset, spec2.getType());
        Assert.assertEquals("10001", spec2.getValue());

        VersionSpec spec3 = VersionSpec.create(Integer.MAX_VALUE);
        Assert.assertEquals(VersionSpec.Type.Changeset, spec3.getType());
        Assert.assertEquals(Integer.toString(Integer.MAX_VALUE), spec3.getValue());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testCreate_int_0() {
        VersionSpec spec1 = VersionSpec.create(0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testCreate_int_below0() {
        VersionSpec spec1 = VersionSpec.create(-100);
    }

    @Test(expected = RuntimeException.class)
    public void testCreate_null_date() {
        VersionSpec spec1 = VersionSpec.create((Date) null);
    }

    @Test
    public void testCreate_string() {
        VersionSpec spec1 = VersionSpec.create("C10001");
        Assert.assertEquals(VersionSpec.Type.Changeset, spec1.getType());
        Assert.assertEquals("10001", spec1.getValue());

        VersionSpec spec2 = VersionSpec.create("T");
        Assert.assertEquals(VersionSpec.Type.Latest, spec2.getType());
        Assert.assertEquals("", spec2.getValue());

        VersionSpec spec3 = VersionSpec.create("D2007-01-01T04:00");
        Assert.assertEquals(VersionSpec.Type.DateTime, spec3.getType());
        Assert.assertEquals("2007-01-01T04:00", spec3.getValue());

        VersionSpec spec4 = VersionSpec.create("LMyLabel");
        Assert.assertEquals(VersionSpec.Type.Label, spec4.getType());
        Assert.assertEquals("MyLabel", spec4.getValue());

        VersionSpec spec5 = VersionSpec.create("Wmyworkspace;me");
        Assert.assertEquals(VersionSpec.Type.Workspace, spec5.getType());
        Assert.assertEquals("myworkspace;me", spec5.getValue());
    }

    @Test
    public void testCreate_string_empty() {
        VersionSpec spec = VersionSpec.create((String) null);
        Assert.assertEquals(VersionSpec.Type.Latest, spec.getType());
        Assert.assertEquals("", spec.getValue());

        spec = VersionSpec.create("");
        Assert.assertEquals(VersionSpec.Type.Latest, spec.getType());
        Assert.assertEquals("", spec.getValue());
    }

    @Test
    public void testCreate_string_default() {
        VersionSpec spec2 = VersionSpec.create("Jstuff");
        Assert.assertEquals(VersionSpec.Type.Latest, spec2.getType());
        Assert.assertEquals("", spec2.getValue());
    }

    @Test
    public void testToString() {
        VersionSpec spec3 = VersionSpec.create("D2007-01-01T04:00");
        Assert.assertEquals(VersionSpec.Type.DateTime, spec3.getType());
        Assert.assertEquals("2007-01-01T04:00", spec3.getValue());
        Assert.assertEquals("D2007-01-01T04:00", spec3.toString());
    }

    @Test
    public void testRange_create() {
        VersionSpec.Range range = VersionSpec.Range.create("C23~D2007-01-01T04:00");
        Assert.assertEquals(VersionSpec.Type.Changeset, range.getStart().getType());
        Assert.assertEquals("23", range.getStart().getValue());
        Assert.assertEquals(VersionSpec.Type.DateTime, range.getEnd().getType());
        Assert.assertEquals("2007-01-01T04:00", range.getEnd().getValue());

        range = VersionSpec.Range.create("C23~C24");
        Assert.assertEquals(VersionSpec.Type.Changeset, range.getStart().getType());
        Assert.assertEquals("23", range.getStart().getValue());
        Assert.assertEquals(VersionSpec.Type.Changeset, range.getEnd().getType());
        Assert.assertEquals("24", range.getEnd().getValue());
    }

    @Test
    public void testRange_create_singleVersion() {
        VersionSpec.Range range = VersionSpec.Range.create("C23");
        Assert.assertEquals("C23~C23", range.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRange_create_fails() {
        VersionSpec.Range range = VersionSpec.Range.create("");
    }

    @Test
    public void testRange_toString() {
        VersionSpec.Range range = new VersionSpec.Range(VersionSpec.create(23), VersionSpec.create(24));
        Assert.assertEquals("C23~C24", range.toString());
    }
}
