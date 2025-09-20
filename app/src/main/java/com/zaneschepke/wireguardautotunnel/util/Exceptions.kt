package com.zaneschepke.wireguardautotunnel.util

class InvalidFileExtensionException : Exception() {
    private fun readResolve(): Any = InvalidFileExtensionException()
}

class FileReadException : Exception() {
    private fun readResolve(): Any = FileReadException()
}

class ConfigExportException : Exception() {
    private fun readResolve(): Any = ConfigExportException()
}
