# Transaction

## 特性

Atomic
Consistency
Iso
Duration

## 隔离级别

Read uncommited
Read commited
Repaired Read
Serial

## Propagation

REQUIRED: Support a current transaction, create a new one if none exists.
SUPPORTS: Support a current transaction, execute non-transactionally if none exists.
MANDATORY: Support a current transaction, throw an exception if none exists.
REQUIRES_NEW: Create a new transaction, and suspend the current transaction if one exists.
NOT_SUPPORTED: Execute non-transactionally, suspend the current transaction if one exists.
NEVER: Execute non-transactionally, throw an exception if a transaction exists.
NESTED: Execute within a nested transaction if a current transaction exists, behave like REQUIRED otherwise.
