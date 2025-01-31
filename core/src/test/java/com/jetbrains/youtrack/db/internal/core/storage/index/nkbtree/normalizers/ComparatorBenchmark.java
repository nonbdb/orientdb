package com.jetbrains.youtrack.db.internal.core.storage.index.nkbtree.normalizers;

import com.jetbrains.youtrack.db.internal.common.comparator.ByteArrayComparator;
import com.jetbrains.youtrack.db.internal.common.comparator.UnsafeByteArrayComparator;
import com.jetbrains.youtrack.db.internal.core.index.CompositeKey;
import com.jetbrains.youtrack.db.api.schema.PropertyType;
import java.text.Collator;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Measurement(iterations = 1, batchSize = 1)
@Warmup(iterations = 1, batchSize = 1)
@Fork(1)
public class ComparatorBenchmark {

  KeyNormalizer keyNormalizer;

  public static void main(String[] args) throws RunnerException {
    final var opt =
        new OptionsBuilder()
            .include("ComparatorBenchmark.*")
            .addProfiler(StackProfiler.class, "detailLine=true;excludePackages=true;period=1")
            .jvmArgs("-server", "-XX:+UseConcMarkSweepGC", "-Xmx4G", "-Xms1G")
            // .result("target" + "/" + "results.csv")
            // .param("offHeapMessages", "true""
            // .resultFormat(ResultFormatType.CSV)
            .build();
    new Runner(opt).run();
  }

  final ByteArrayComparator arrayComparator = new ByteArrayComparator();
  final UnsafeByteArrayComparator byteArrayComparator = new UnsafeByteArrayComparator();

  byte[] negative;
  byte[] zero;
  byte[] positive;

  @Setup(Level.Iteration)
  public void setup() {
    keyNormalizer = new KeyNormalizer();

    negative = getNormalizedKeySingle(-62, PropertyType.INTEGER);
    zero = getNormalizedKeySingle(0, PropertyType.INTEGER);
    positive = getNormalizedKeySingle(5, PropertyType.INTEGER);
  }

  @Benchmark
  public void comparatorByteArrayNegative() throws Exception {
    byteArrayComparator.compare(negative, zero);
  }

  @Benchmark
  public void comparatorByteArrayPositive() throws Exception {
    byteArrayComparator.compare(positive, zero);
  }

  @Benchmark
  public void comparatorByteArrayEqual() throws Exception {
    byteArrayComparator.compare(zero, zero);
  }

  @Benchmark
  public void comparatorUnsafeByteArrayNegative() throws Exception {
    arrayComparator.compare(negative, zero);
  }

  @Benchmark
  public void comparatorUnsafeByteArrayPositive() throws Exception {
    arrayComparator.compare(positive, zero);
  }

  @Benchmark
  public void comparatorUnsafeByteArrayEqual() throws Exception {
    arrayComparator.compare(zero, zero);
  }

  private byte[] getNormalizedKeySingle(final int keyValue, final PropertyType type) {
    final var compositeKey = new CompositeKey();
    compositeKey.addKey(keyValue);
    Assert.assertEquals(1, compositeKey.getKeys().size());

    final var types = new PropertyType[1];
    types[0] = type;

    return keyNormalizer.normalize(compositeKey, types, Collator.NO_DECOMPOSITION);
  }
}
