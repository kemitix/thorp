package net.kemitix.s3thorp

import com.typesafe.scalalogging.LazyLogging

trait Logging extends LazyLogging {

    def log1(message: String)(implicit config: Config): Unit = if (config.verbose >= 1) logger.info(message)

    def log2(message: String)(implicit config: Config): Unit = if (config.verbose >= 2) logger.info(message)

    def log3(message: String)(implicit config: Config): Unit = if (config.verbose >= 3) logger.info(message)

    def log4(message: String)(implicit config: Config): Unit = if (config.verbose >= 4) logger.info(message)

    def log5(message: String)(implicit config: Config): Unit = if (config.verbose >= 5) logger.info(message)

    def warn(message: String): Unit = logger.warn(message)

    def error(message: String): Unit = logger.error(message)

}
