package com.mlacker.samples.java.util.concurrent.locks

interface ReadWriteLock {

    val readLock: Lock

    val writeLock: Lock
}