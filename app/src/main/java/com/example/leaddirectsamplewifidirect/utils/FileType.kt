package com.example.leaddirectsamplewifidirect.utils

import java.io.File

sealed class FileType {
    object VIDEO : FileType()
    object IMAGE : FileType()
    object PDF : FileType()
}
