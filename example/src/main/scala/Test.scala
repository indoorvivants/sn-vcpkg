import scalanative.unsafe.*

@extern
object api:
  def test_binding(): CString = extern

@main def hello = 
  println(api.test_binding())

