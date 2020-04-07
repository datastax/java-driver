/*
 * Copyright DataStax, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datastax.oss.driver.internal.core.os;

import java.util.Locale;

/** A gateway to perform system calls. */
public class Native {

  /* Copied from equivalent op in jnr.ffi.Platform.  We have to have this here as it has to be defined
   * before it's (multiple) uses in determineCpu() */
  private static final Locale LOCALE = java.util.Locale.ENGLISH;

  private static final NativeImpl IMPL = new JnrNativeImpl();

  @SuppressWarnings("VariableNameSameAsType")
  private static final CPU CPU = determineCPU();

  /** Whether {@link Native#currentTimeMicros()} is available on this system. */
  public static boolean isCurrentTimeMicrosAvailable() {
    return IMPL.gettimeofdayAvailable();
  }

  /**
   * The current time in microseconds, as returned by libc.gettimeofday(); can only be used if
   * {@link #isCurrentTimeMicrosAvailable()} is true.
   */
  public static long currentTimeMicros() {
    return IMPL.gettimeofday();
  }

  public static boolean isGetProcessIdAvailable() {
    return IMPL.getpidAvailable();
  }

  public static int getProcessId() {
    return IMPL.getpid();
  }

  /**
   * Returns the current processor architecture the JVM is running on. This value should match up to
   * what's returned by jnr-ffi's Platform.getCPU() method.
   *
   * @return the current processor architecture.
   */
  public static String getCPU() {
    return CPU.toString();
  }

  /* The remainder of this class is copied from jnr.ffi.Platform in jnr-ffi version 2.1.10.
   * We copy it manually here in order to avoid introducing an extra dependency merely for the sake of
   * evaluating some system properties.
   *
   * jnr-ffi copyright notice follows:
   *
   * Copyright (C) 2008-2010 Wayne Meissner
   *
   * This file is part of the JNR project.
   *
   * Licensed under the Apache License, Version 2.0 (the "License");
   * you may not use this file except in compliance with the License.
   * You may obtain a copy of the License at
   *
   *    http://www.apache.org/licenses/LICENSE-2.0
   *
   * Unless required by applicable law or agreed to in writing, software
   * distributed under the License is distributed on an "AS IS" BASIS,
   * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   * See the License for the specific language governing permissions and
   * limitations under the License.
   */
  /** The supported CPU architectures. */
  private enum CPU {
    /*
     * <b>Note</b> The names of the enum values are used in other parts of the
     * code to determine where to find the native stub library.  Do NOT rename.
     */

    /** 32 bit legacy Intel */
    I386,

    /** 64 bit AMD (aka EM64T/X64) */
    X86_64,

    /** 32 bit Power PC */
    PPC,

    /** 64 bit Power PC */
    PPC64,

    /** 64 bit Power PC little endian */
    PPC64LE,

    /** 32 bit Sun sparc */
    SPARC,

    /** 64 bit Sun sparc */
    SPARCV9,

    /** IBM zSeries S/390 */
    S390X,

    /** 32 bit MIPS (used by nestedvm) */
    MIPS32,

    /** 32 bit ARM */
    ARM,

    /** 64 bit ARM */
    AARCH64,

    /**
     * Unknown CPU architecture. A best effort will be made to infer architecture specific values
     * such as address and long size.
     */
    UNKNOWN;

    /**
     * Returns a {@code String} object representing this {@code CPU} object.
     *
     * @return the name of the cpu architecture as a lower case {@code String}.
     */
    @Override
    public String toString() {
      return name().toLowerCase(LOCALE);
    }
  }

  private static CPU determineCPU() {
    String archString = System.getProperty("os.arch");
    assert (archString != null);
    assert (archString.toUpperCase(LOCALE) != null);
    assert (archString.toLowerCase(LOCALE) != null);
    if (equalsIgnoreCase("x86", archString)
        || equalsIgnoreCase("i386", archString)
        || equalsIgnoreCase("i86pc", archString)
        || equalsIgnoreCase("i686", archString)) {
      return CPU.I386;
    } else if (equalsIgnoreCase("x86_64", archString) || equalsIgnoreCase("amd64", archString)) {
      return CPU.X86_64;
    } else if (equalsIgnoreCase("ppc", archString) || equalsIgnoreCase("powerpc", archString)) {
      return CPU.PPC;
    } else if (equalsIgnoreCase("ppc64", archString) || equalsIgnoreCase("powerpc64", archString)) {
      if ("little".equals(System.getProperty("sun.cpu.endian"))) {
        return CPU.PPC64LE;
      }
      return CPU.PPC64;
    } else if (equalsIgnoreCase("ppc64le", archString)
        || equalsIgnoreCase("powerpc64le", archString)) {
      return CPU.PPC64LE;
    } else if (equalsIgnoreCase("s390", archString) || equalsIgnoreCase("s390x", archString)) {
      return CPU.S390X;
    } else if (equalsIgnoreCase("aarch64", archString)) {
      return CPU.AARCH64;
    } else if (equalsIgnoreCase("arm", archString) || equalsIgnoreCase("armv7l", archString)) {
      return CPU.ARM;
    }

    // Try to find by lookup up in the CPU list
    for (CPU cpu : CPU.values()) {
      if (equalsIgnoreCase(cpu.name(), archString)) {
        return cpu;
      }
    }

    return CPU.UNKNOWN;
  }

  private static boolean equalsIgnoreCase(String s1, String s2) {
    return s1.equalsIgnoreCase(s2)
        || s1.toUpperCase(LOCALE).equals(s2.toUpperCase(LOCALE))
        || s1.toLowerCase(LOCALE).equals(s2.toLowerCase(LOCALE));
  }
}
