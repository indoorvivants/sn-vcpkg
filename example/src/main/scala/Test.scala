import scalanative.unsafe.*

import cjson.functions.*
import cjson.types.*

import czmq.functions.*
import czmq.types.*

import libuv.functions.*
import libuv.types.*

import scalanative.unsigned.*

@main def hello = 

  Zone { implicit z =>
    val loop = uv_loop_t()

    uv_loop_init(loop)

    val timer = uv_timer_t()

    uv_timer_init(loop, timer)

    val cb = uv_timer_cb {
      CFuncPtr1.fromScalaFunction { (t: Ptr[uv_timer_t]) =>
        val uuid = zuuid_new()

        val json = cJSON_CreateObject()
        val jsonString = cJSON_CreateString(zuuid_str(uuid))
        
        cJSON_AddItemToObject(json, c"uuid", jsonString)

        println(fromCString(cJSON_Print(json)))

        cJSON_Delete(json)
      }
    }

    uv_timer_start(timer, cb, 500.toULong, 500.toULong)

    uv_run(loop, uv_run_mode.UV_RUN_DEFAULT)

    uv_loop_close(loop)
  }
