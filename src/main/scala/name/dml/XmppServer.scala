package name.dml

import java.net.InetSocketAddress
import java.nio.{ByteBuffer,ByteOrder}
import java.nio.channels.{Selector,SelectionKey,ServerSocketChannel,SocketChannel,Channel}
import java.nio.channels.SelectionKey._
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import scala.actors.Actor
import scala.actors.Actor._
import scala.collection.mutable.Map
import scala.collection.jcl.Conversions._
import scala.reflect.Manifest

class XmppServerRegistry extends DispatcherComponent with HandlerComponent {
  // dispatcher depenencies
  val selector = Selector.open
  val channel: ServerSocketChannel = ServerSocketChannel.open
  val port = 5222
  def handle(channel: SocketChannel) = connection(channel)
  channel.socket bind (new InetSocketAddress (port))
  channel configureBlocking false
  channel.register(selector, channel.validOps)
  
  // charconnection dependencies
  def alloc(capacity: Int) = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder)
  def charset = Charset.availableCharsets.get("UTF-8")
  
  // start
  val dispatcher = new Dispatcher
}

trait DispatcherComponent {
  val selector: Selector
  def handle(channel: SocketChannel): Actor
  
  class Dispatcher extends Actor {
    val handlers = Map(): Map[Channel, Actor]
    
    this.start
    
    def act = {
      loop {
        selector.selectNow match {
          case 0 => () // nothing to select for now
          case _ =>
            val itr = selector.selectedKeys.iterator
          	while (itr.hasNext) {
          	  val key = itr.next
          	  itr.remove
          	  key match {
          	    case _ if key.isAcceptable => { key.channel match {
          	      case ssc: ServerSocketChannel => {
          	        val channel = ssc.accept
          	    	channel configureBlocking false
          	    	channel.register(selector, OP_READ)
          	    	val handler = handle(channel)
          	    	handlers += channel -> handler
          	    	handler.start
          	      }
                 case _ => println("error") // FIXME
          	    }}
                case _ if key.isReadable => { 
                  handlers(key.channel) ! Read 
                }
          	  }
          	}
        }
      }
    }
  }
}

sealed abstract class HandlerMsg
case object Read extends HandlerMsg

trait HandlerComponent {
  def alloc(capacity: Int): ByteBuffer
  def charset: Charset
  
  def connection(channel: SocketChannel) = new CharConnection(channel)
  
  class CharConnection(private val channel: SocketChannel) extends Actor {
    val buffer = alloc(8192)
    val decoder = charset.newDecoder
    
    def act = {
      loop {
        react {
          case Read => {
            (channel read buffer) match {
              case -1 => { 
                channel.close
                exit
              }
              case 0 => ()
              case n: Int => {
                buffer.flip
                val in = decoder decode buffer
//                val arr = new Array[Byte](n)
//                buffer.get(arr)
//                val str = new String(arr, "UTF-8")
                buffer.clear
              }
            }
          }
        }
      }
    }
  }
}