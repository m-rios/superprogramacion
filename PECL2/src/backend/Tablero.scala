package backend

import scala.util.Random
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

import frontend.AppGUI


class Tablero(xi:Int,yi:Int,dificulty:Int) {
  val x = xi
  val y = yi
  val content = populate(Nil,x*y)
  val toChar = "_0123456"
  def random = Random.nextInt(dificulty)
  
  //cambio de coordenadas x,y a una coordenada lineal
  def linear_coords(x:Int, y:Int):Int = y * this.x + x

  //devuelve el contenido de la tabla limpio
  def get_clean_content:List[Int] = clean_table(content, 0)._1
  
  //Convierte una List[Int] de scala a una Lista de Java [Integer]
  implicit def toIntegerList( lst: List[Int] ) = seqAsJavaList( lst.map( i => i:java.lang.Integer ) )
  //Convierte una Lista de Java [Integer] a una List[Int] de scala
  implicit def toIntList( lst: java.util.List[Integer] ):List[Int] = lst.asScala.map { i => i:Int }.toList
  
  //Crea un tablero con datos aleatorios
  def populate(l:List[Int],n:Int):List[Int] = n match {
    case 0 => l
    case _ => populate(random::l,n-1)
  }
  
  //Inserta el color col en la posicion lineal pos de la lista l
  def insert(col:Int,pos:Int,l:List[Int]):List[Int] = {
    if(l.isEmpty) Nil
    else if (pos==0) col::l.tail
    else l.head::insert(col,(pos-1),l.tail)
  }
  
  //recorre la lista y cuenta las casillas borradas (-1)
  def puntuar(l:List[Int]):Int = {
    if (l.isEmpty) 0
    else {
      if (l.head == -1) 1 + puntuar(l.tail)
      else puntuar(l.tail)
    }    
  }

  //Limpia la tabla para que no haya 3 caras del mismo color seguidas
  def clean_table(l:List[Int],puntuacion:Int):(List[Int],Int) = {
    //'bajar' introduce valores aleatorios, hay que comprobar que esos
    //valores no se agrupen en > 2 iguales, si no hay q volver a limpiar
    if (is_clean(l)) (l,puntuacion)
    else{
      //limpiar verticalmente
      val tras = transponer(l, Nil, 0)
      val aux = clean_aux(tras,tras,0)
      //limpiar horizontalmente
      val cleaned = clean_aux(l,transponer(aux,Nil,0),0)
      println("\n\nJugada: ")
      print_aux(cleaned, 1)
      val j_pun = puntuar(cleaned)
      val bajada = bajar(cleaned,0)
      println("\n\nBaja los colores y genera nuevos colores aleatorios: ")
      print_aux(bajada, 1)
      clean_table(bajada,puntuacion+j_pun*10)
    }    
  }
  
  //Limpia la tabla en un sentido (horizontal o vertical)
  def clean_aux(l:List[Int],nueva:List[Int],row:Int):List[Int] = row match{
    case this.y => nueva
    case _ => clean_aux(l,limpiar_linea(l,nueva,row*x),row+1)
  }
  
  //pone a -1 las casillas de una línea que tengan 2 o más vecinos iguales
  //devuelve la lista con la linea 'limpia' y el número de bolas eliminadas
 def limpiar_linea(original:List[Int],nueva:List[Int],pos:Int):List[Int] = (x-(pos%x)) match{
    //ya se ha evaluado la 3a última
    case 2 => nueva
    //aún no se ha evaluado la 3a última
    case _ => {
      if (check_next3(original,get_color(original,pos),pos,1) == 3) {
        val nueva2 = insert(-1,pos,insert(-1,pos+1,insert(-1,pos+2,nueva)))
        limpiar_linea(original,nueva2,pos+1)
      }else limpiar_linea(original,nueva,pos+1)
    }
  }
 
  //calcula el número de iguales de las siguientes 3 casillas
  def check_next3(l:List[Int],col:Int,pos:Int,c:Int):Int = c match{
    case 3 => if (get_color(l,pos) == col) 1 else 0
    case _ => if (get_color(l,pos) == col) 1 + check_next3(l,get_color(l,pos),pos+1,c+1)
              else 0 + check_next3(l,get_color(l,pos),pos+1,c+1)      
  }
  
  //hace caer las casillas no vacías de todo el tablero
  def bajar(l:List[Int],j:Int):List[Int] = {
    if (j == x) l
    else
      bajar(bajar_fila(l,linear_coords(j,y-1)),j+1)
  }
  //hace caer las casillas no vacías de una columna
  def bajar_fila(l:List[Int],pos:Int):List[Int] = {
	  //caso base: fila de arriba
	  if (pos < x) {
		  if (get_color(l, pos) < 0) insert(random,pos,l) else l  
	  }
	  //fila intermedia
	  else {
		  val color = get_color(l,pos)
				  
		  if ((color < 0)&&(get_color(l,pos-x)<0))
      {
        val sup = superior(l,pos-x)
        if(sup >= 0)
			    bajar_fila(insert(color,superior(l,pos-x),insert(get_color(l,superior(l,pos-x)),pos,l)),pos-x)
        else bajar_fila(insert(random,pos,l),pos-x)
      }else if (color < 0)
        bajar_fila(insert(color,pos-x,insert(get_color(l,pos-x),pos,l)),pos-x)
      else
        bajar_fila(l,pos-x)
	  }
  }
  
  //devuelve la pos del primer superior no nulo. Si no existe devuelve -1. Hay que pasarle pos-1
  def superior(l:List[Int],pos:Int):Int = {
    //primera fila valor positivo
    if ((pos < x)&&(get_color(l,pos)>0)) pos
    //primera fila vacia
    else if((pos < x)&&(get_color(l,pos)<0)) -1
    //fila intermedia 
    else if (get_color(l,pos) < 0 )
      superior(l,pos-x) 
      else pos     
  }
  
  //Comprueba si el tablero está limpio
  def is_clean(l:List[Int]):Boolean = {
    return (check_horizontal(l,0,1) && check_vertical(l,0,1))
  }
  
  //Comprueba si hay filas de al menos 3 seguidos en las columnas
  def check_vertical(l:List[Int], pos:Int, c:Int):Boolean = {
    val transpuesta = transponer(l, Nil, 0)
    return check_horizontal(transpuesta, pos, c)
  }
  
  //las filas son columnas y las columnas son filas
  def transponer(li:List[Int], l:List[Int], n:Int):List[Int] = {
    val siguiente = next_vertical(n)
    //println("lista: " + l + "\npos: " + n + "\nsiguiente: " + siguiente)
    if(siguiente > 0) {
      //println(">>> pos NO es el último de la fila")
      transponer(li, l:::List(get_color(li, n)),next_vertical(n))
    } else if (siguiente == (-1)) {
      //println(">>> pos ES el último de la fila")
      transponer(li, l:::List(get_color(li, n)), n - this.x * (this.y-1) + 1)
    } else {
      //println(">>> pos ES el último del tablero, se acaba")
      l:::List(get_color(li, n))
    }
  }
  
  //Comprueba si hay filas de al menos 3 seguidos en las columnas
  def check_horizontal(l:List[Int], pos:Int, c:Int):Boolean = {
    if(c == 3) {
      //println(">>> contador llega a 3 --> devuelve false")
      return false
    }
    val siguiente = next_horizontal(pos)
    //comprueba el siguiente
    if(siguiente > 0) {
      //println(">>> pos NO es el último de la fila")
      if(get_color(l,pos) == get_color(l,siguiente)) {
        //println(">>> el color ES igual que el siguiente. pasamos valor c = " + (c+1))
        if (!check_horizontal(l, siguiente, c+1)) return false else return true
      } else {
        //println(">>> el color NO es igual que el siguiente. pasamos valor c = 1")
        if (!check_horizontal(l, siguiente, 1)) return false else return true
      }
    } else if(siguiente == (-1)){
      //cambia a la siguiente fila
      //println(">>> pos ES el último de la fila. pasamos pos+1 y c = 1")
      if (!check_horizontal(l, pos+1, 1)) return false else return true
    } else {
      //fin del tablero
      //println(">>> pos ES el último del tablero, se acaba --> devuelve false")
      return true
    }
  } 
  
  //Devuelve la siguiente posicion lineal vertical a una dada, -1 si es la última
  def next_vertical(pos:Int):Int = {
    if ((pos+1) == (this.x*this.y)) (-2) //se ha terminado el tablero
    else if(pos >= (this.x * (this.y-1))) (-1) //se ha terminado la columna
    else pos + this.x
  }
  
  //Devuelve la siguiente posicion lineal horizontal a una dada, -1 si es la última
  def next_horizontal(pos:Int):Int = {
    if ((pos+1) == (this.x*this.y)) (-2) //se ha terminado el tablero
    else if(((pos+1) % this.x) == 0) (-1) //se ha terminado la fila
    else pos+1
  }
  
  //Devuelve el entero que representa el color en una posición
  def get_color(l:List[Int], pos:Int):Int = {
    if (pos==0) l.head
    else get_color(l.tail, pos-1)
  }
  
  //Recibe como parámetros las coordenadas de la cara que mueve y la dirección (se calcularán las coordenadas)
  def move(l:List[Int], x:Int, y:Int, dir:Char):List[Int] = {
    val origen = linear_coords(x, y)
    val destino = dir match {
      case 'N' => linear_coords(x, y-1)
      case 'S' => linear_coords(x, y+1)
      case 'E' => linear_coords(x+1, y)
      case 'O' => linear_coords(x-1, y)
    }
    val origenColor = get_color(l, origen)
    println(">>> Origen: " + origen)
    println(">>> Color Origen: " + toChar.charAt(origenColor+1))
    val destinoColor = get_color(l, destino)
    println(">>> Destino: " + destino)
    println(">>> Color Destino: " + toChar.charAt(destinoColor+1))
    return insert(origenColor, destino, insert(destinoColor, origen, l))
  }
  
  //Método principal recursivo para las jugadas
  def play(l:List[Int],m:Int,p:Int):Int = {
    //se comprueba si quedan movimietos
    if (m == 0){
      println("\n\nTablero:")
      print_aux(l,1)
      p
    }      
    else try{
      //sleep para captar la interrupción
      Thread.sleep(1)
      println("\n\nTablero: ")
      print_aux(l,1)
      println("\nPuntos acumulados: " + p)
      println("Coordenada X: ")
      val x = io.StdIn.readInt
      Thread.sleep(1)
      println("Coordenada Y: ")
      val y = io.StdIn.readInt
      Thread.sleep(1)
      println("Direccion (N, S, E, O): ")
      val dir = io.StdIn.readChar.toUpper
      Thread.sleep(1)
      println(">>> Coordenadas: " + x + ", " + y + " Dirección: " + dir)
      val lista_movimiento = move(l, x, y, dir)
      //si no se puede limpiar nada la jugada no es válida, 
      //por lo que llamamos al método play con su mismo valor de entrada
      if(is_clean(lista_movimiento)) {println(">>> Jugada no válida !!!");play(l,m,p)}
      else{
        val (lista_limpia,np) = clean_table(lista_movimiento,p)
        play(lista_limpia,m-1,p+np)
      }
    } catch{ case e: InterruptedException => return p}
      
  }
  
  //Método principal que se llama desde la GUI en JAVA, recibe una lista con el movimiento hecho y devuelve el tablero limpio resultatnte, 
  //y los puntos (en la primera posicion de la lista)
  def play_GUI(grid:java.util.List[java.lang.Integer]):java.util.List[Integer] = {
    
    val scalaGrid = toIntList(grid)
    
    if (is_clean(scalaGrid)) return null
    else {
      val (l,p) = play_GUI_aux(toIntList(grid),0)
      toIntegerList(p::l)
    }
  }
  
  //metodo auxiliar recursivo para GUI
  def play_GUI_aux(l:List[Int],p:Int):(List[Int],Int) = {      
    if(is_clean(l)) return (l,p)
    else{
      val (lista_limpia,np) = clean_table(l,p)
      play_GUI_aux(lista_limpia,p+np)
    }      
  }
  
  //Funciones de imprimir
  def print_aux(l:List[Int],n:Int):Unit = {
    if(l.isEmpty) return
    if((n%x==0) && (n < x*y)) {println(toChar.charAt(l.head+1)); print_aux(l.tail,n+1)}
    else {scala.Predef.print(toChar.charAt(l.head+1)+" "); print_aux(l.tail,n+1)}
  }

}