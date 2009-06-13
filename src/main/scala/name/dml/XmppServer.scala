package name.dml

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.{Selector,SelectionKey,ServerSocketChannel,SocketChannel,Channel}
import java.nio.channels.SelectionKey._
import scala.actors.Actor
import scala.actors.Actor._
import scala.collection.mutable.Map
import scala.collection.jcl.Conversions._
import scala.reflect.Manifest

class XmppServerRegistry extends DispatcherComponent with HandlerComponent {
  val selector = Selector.open
  val channel: ServerSocketChannel = ServerSocketChannel.open
  val port = 5222
  
  def alloc(capacity: Int) = ByteBuffer.allocateDirect(capacity)
  
  channel.socket bind (new InetSocketAddress (port))
  channel configureBlocking false
  channel.register(selector, channel.validOps)
  
  val dispatcher = new Dispatcher
  dispatcher.start
}

trait DispatcherComponent {
  val selector: Selector
  def handle(channel: SocketChannel): Actor
  
  class Dispatcher extends Actor {
    val handlers = Map(): Map[Channel, Actor]
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
case object Init extends HandlerMsg
case object Read extends HandlerMsg

trait HandlerComponent {
  def alloc(capacity: Int): ByteBuffer
  
  def handle(channel: SocketChannel) = new Handler(channel)
  
  class Handler(private val channel: SocketChannel) extends Actor {
    val buffer = alloc(8192)
    def act = {
      loop {
        react {
          case Read => {
            (channel read buffer) match {
              case -1 => { 
                channel.close
                exit
              }
              case _ => {
                println(buffer.asCharBuffer)
                buffer.clear
              }
            }
          }
        }
      }
    }
  }
}