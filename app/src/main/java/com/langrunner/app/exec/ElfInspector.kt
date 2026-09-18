package com.langrunner.app.exec

import java.io.File

/**
 * Reads just the ELF header to determine whether a file is a valid ELF
 * executable and what CPU architecture it targets, without executing it.
 * Used to catch a very common failure mode: a binary cross-compiled for the
 * wrong architecture (e.g. a plain `cargo build --release` on a desktop
 * produces an x86_64 binary, which cannot run on an ARM64 Android device).
 */
object ElfInspector {

    private const val EM_ARM = 0x28      // 40  - ARM32
    private const val EM_AARCH64 = 0xB7  // 183 - ARM64 (aarch64)
    private const val EM_386 = 0x03
    private const val EM_X86_64 = 0x3E

    data class ElfInfo(val isElf: Boolean, val is64Bit: Boolean, val machine: Int)

    fun inspect(file: File): ElfInfo {
        file.inputStream().use { input ->
            val header = ByteArray(20)
            val read = input.read(header)
            val isElf = read == 20 &&
                header[0] == 0x7F.toByte() && header[1] == 'E'.code.toByte() &&
                header[2] == 'L'.code.toByte() && header[3] == 'F'.code.toByte()

            if (!isElf) return ElfInfo(isElf = false, is64Bit = false, machine = 0)

            val is64Bit = header[4].toInt() == 2
            // e_machine is a 2-byte little-endian value at header offset 18
            val machine = (header[18].toInt() and 0xFF) or ((header[19].toInt() and 0xFF) shl 8)
            return ElfInfo(isElf = true, is64Bit = is64Bit, machine = machine)
        }
    }

    fun architectureName(machine: Int): String = when (machine) {
        EM_AARCH64 -> "ARM64"
        EM_ARM -> "ARM32"
        EM_X86_64 -> "x86_64"
        EM_386 -> "x86"
        else -> "unknown (0x${machine.toString(16)})"
    }

    fun isSupported(info: ElfInfo): Boolean = info.isElf && info.machine == EM_AARCH64
}
