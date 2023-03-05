package simple

import scalanative.unsafe.*

type CJSON = Ptr[Byte]

def cJSON_Parse(str: CString): Ptr[Byte] = extern
def cJSON_Print(json: CJSON): CString = extern

type MARKDOWN = Ptr[Byte]

def cmark_parse_document(str: CString, size: Int, flags: Int): MARKDOWN = extern
def cmark_render_html(root: MARKDOWN, flags: Int): CString = extern

@main def hello = 
  val json = cJSON_Parse(c"[1, true]")
  assert(fromCString(cJSON_Print(json)) == "[1, true]")
  
  Zone { implicit z => 
    val str = "Hello, _world_"
    val markdown = cmark_parse_document(toCString(str), str.length, 0)
    val html = fromCString(cmark_render_html(markdown, 0)).trim
    assert(html == "<p>Hello, <em>world</em></p>")
  }

