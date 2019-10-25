package net.kemitix.thorp

import java.time.Instant

package object domain {
  type Hashes       = Map[HashType, MD5Hash]
  type LastModified = Instant
}
