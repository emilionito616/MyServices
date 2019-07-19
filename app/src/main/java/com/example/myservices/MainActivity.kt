package com.example.myservices

import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.lang.Exception
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URI
import java.net.URL

class MainActivity : AppCompatActivity() {
    var wsConsultar : String="http://172.16.252.50/Servicios/MostrarAlumno.php"
    var wsInsertar : String="http://172.16.252.50/Servicios/InsertarAlumno.php"
    var hilo: ObtenerServicioWeb? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun consultaNoControl(v:View){
        if (etNoControl.text.isEmpty()){
            Toast.makeText(this, "Falta ingresar el numero de control", Toast.LENGTH_SHORT).show()
            etNoControl.requestFocus()
        }else{
            val no = etNoControl.text.toString()
            hilo = ObtenerServicioWeb()
            hilo?.execute("Consulta",no,"","","")//Solamente se ponen los campos o el campo por el cual se va a buscar y los demas vacios
        }
    }

    fun insertaAlumno(v:View){
        if (etNoControl.text.isEmpty() || etCarrera.text.isEmpty() || etNombre.text.isEmpty() || etTelefono.text.isEmpty()){
            Toast.makeText(this, "Falta ingresar todos los datos", Toast.LENGTH_SHORT).show()
            etNoControl.requestFocus()
        }else{
            val no = etNoControl.text.toString()
            val carr = etCarrera.text.toString()
            val nom = etNombre.text.toString()
            val tel = etTelefono.text.toString()
            hilo = ObtenerServicioWeb()
            hilo?.execute("insertar",no,carr,nom,tel)
        }
    }

    inner class ObtenerServicioWeb(): AsyncTask<String,String,String>(){
        override fun doInBackground(vararg params: String?): String {
            var Url : URL?=null
            var sResultado=""
            try {
                val urlConn : HttpURLConnection
                val prinout : DataInputStream
                val input : DataInputStream
                if(params[2].toString().isEmpty() && params[3].toString().isEmpty()){
                    Url = URL(wsConsultar)
                }else{
                    Url = URL(wsInsertar)
                }
                urlConn = Url.openConnection() as HttpURLConnection//Se abre la conexion para nuestro web service
                urlConn.doInput = true //Va a llevar parametros de entrada
                urlConn.doOutput = true //Va a llevar parametros de salida
                urlConn.useCaches = false
                urlConn.setRequestProperty("Content-Type","aplication/json")//Tipo de informacion que va a manejar en este caso es de tipo Json
                urlConn.setRequestProperty("Accept", "aplication/json")
                urlConn.connect()//Indica que se conecte
                //siguiente codigo es para preparar los siguientes datos a enviar al web service
                val jsonParam=JSONObject()//recibe los parametros para trabajar con el json
                jsonParam.put("nocontrol",params[1])//Se tiene que enviar los parametros como se espera que los reciba y despues el valor
                jsonParam.put("carrera",params[2])
                jsonParam.put("nombre",params[3])
                jsonParam.put("telefono",params[4])
                val us = urlConn.outputStream
                val writer = BufferedWriter(OutputStreamWriter(us,"UTF-8"))
                writer.write(jsonParam.toString())
                writer.flush()
                writer.close()
                val respuesta = urlConn.responseCode
                val result = StringBuilder()//Es para cuando se necesitan concatenar varias variables en unsa sola linea, con un ciclo
                if (respuesta==HttpURLConnection.HTTP_OK){
                    //leer json que regresa el web service
                    val inStream :InputStream= urlConn.inputStream
                    val isReader = InputStreamReader(inStream)
                    val bReader = BufferedReader(isReader)
                    var tempStr : String?
                    while (true){//ciclo que va a estar leyendo
                        tempStr=bReader.readLine()
                        if (tempStr == null){
                            break
                        }
                        result.append(tempStr)
                    }
                    //nos desconectamos del urlConn
                    urlConn.disconnect()
                    sResultado = result.toString()//asignamos a la variable sResult lo que trae result, esto es lo que va a regresar el web service

                }
            }catch (e: MalformedURLException){
                Log.d("NITO",e.message)
            }catch (e: IOException){
                Log.d("NITO",e.message)
            }catch (e: JSONException){
                Log.d("NITO",e.message)
            }catch (e: Exception){
                Log.d("NITO",e.message)
            }
            return sResultado
        }
        override fun onPostExecute(result: String?) {
            var no:String = ""
            var nom: String = ""
            var carr: String = ""
            var tel: String = ""
            super.onPostExecute(result)
            //Parseo de un JSON es lo mismo que leerlo
            try {
                val respuetaJSON = JSONObject(result)//Result es el JSON que se recibe, viene en String y se tiene que convertir con JSONObject
                val resultJSON = respuetaJSON.getString("success")
                when{
                    resultJSON == "200" ->{
                        val alumnoJSON = respuetaJSON.getJSONArray("alumno")//Se necesita declarar un  arreglo porque en JSON estan los datos en un arreglo
                        if (alumnoJSON.length() >= 1){//No es un arreglo, por eso empieza en la posicion 1
                            //Todos son el elemento 0 porque estan en el arreglo en la posicion 0
                            no = alumnoJSON.getJSONObject(0).getString("nocontrol")
                            carr = alumnoJSON.getJSONObject(0).getString("carrera")
                            nom = alumnoJSON.getJSONObject(0).getString("nombre")
                            tel = alumnoJSON.getJSONObject(0).getString("telefono")
                            etNoControl.setText(no)
                            etCarrera.setText(carr)
                            etNombre.setText(nom)
                            etTelefono.setText(tel)
                        }
                    }
                    resultJSON == "201" ->{
                        Toast.makeText(baseContext, "Alumno almacenado en el web service", Toast.LENGTH_SHORT).show()
                        etNoControl.setText("")
                        etCarrera.setText("")
                        etNombre.setText("")
                        etTelefono.setText("")
                        etNoControl.requestFocus()
                    }
                    resultJSON == "204" ->{
                        Toast.makeText(baseContext, "Alumno NO encontrado", Toast.LENGTH_SHORT).show()//baseContext es para cuando estamos afuera de otra clase
                    }
                    resultJSON == "409" ->{
                        Toast.makeText(baseContext, "Error al agregar alumno", Toast.LENGTH_SHORT).show()//baseContext es para cuando estamos afuera de otra clase
                    }
                }
            }catch (e: Exception){
                Log.d("NITO",e.message)
            }
        }
    }
}
