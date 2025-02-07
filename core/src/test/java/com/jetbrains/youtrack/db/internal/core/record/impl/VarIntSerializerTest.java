package com.jetbrains.youtrack.db.internal.core.record.impl;

import static org.junit.Assert.assertEquals;

import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.BytesContainer;
import com.jetbrains.youtrack.db.internal.core.serialization.serializer.record.binary.VarIntSerializer;
import org.junit.Test;

public class VarIntSerializerTest {

  @Test
  public void serializeZero() {
    BytesContainer bytes = new BytesContainer();
    VarIntSerializer.write(bytes, 0);
    bytes.offset = 0;
    assertEquals(VarIntSerializer.readAsLong(bytes), 0L);
  }

  @Test
  public void serializeNegative() {
    BytesContainer bytes = new BytesContainer();
    VarIntSerializer.write(bytes, -20432343);
    bytes.offset = 0;
    assertEquals(VarIntSerializer.readAsLong(bytes), -20432343L);
  }

  @Test
  public void serializePositive() {
    BytesContainer bytes = new BytesContainer();
    VarIntSerializer.write(bytes, 20432343);
    bytes.offset = 0;
    assertEquals(VarIntSerializer.readAsLong(bytes), 20432343L);
  }

  @Test
  public void serializeCrazyPositive() {
    BytesContainer bytes = new BytesContainer();
    VarIntSerializer.write(bytes, 16238);
    bytes.offset = 0;
    assertEquals(VarIntSerializer.readAsLong(bytes), 16238L);
  }

  @Test
  public void serializePosition() {
    BytesContainer bytes = new BytesContainer();
    bytes.offset = VarIntSerializer.write(bytes, 16238);
    assertEquals(VarIntSerializer.readAsLong(bytes), 16238L);
  }

  @Test
  public void serializeMaxLong() {
    BytesContainer bytes = new BytesContainer();
    bytes.offset = VarIntSerializer.write(bytes, Long.MAX_VALUE);
    assertEquals(VarIntSerializer.readAsLong(bytes), Long.MAX_VALUE);
  }

  @Test
  public void serializeMinLong() {
    BytesContainer bytes = new BytesContainer();
    bytes.offset = VarIntSerializer.write(bytes, Long.MIN_VALUE);
    assertEquals(VarIntSerializer.readAsLong(bytes), Long.MIN_VALUE);
  }
}
