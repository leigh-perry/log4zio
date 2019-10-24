package com.leighperry

package object log4zio {

  type Log[A] = LogE[Nothing, A]

}
