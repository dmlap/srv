package name.dml

import org.junit.{Test,Before,After}
import org.junit.runners.JUnit4
import org.junit.runner.RunWith
import org.junit.Assert._
import org.jivesoftware.smack.{XMPPConnection,Chat,MessageListener}
import org.jivesoftware.smack.packet.Message

@RunWith(classOf[JUnit4])
class XmppStreamTest {
  val server = new XmppServerRegistry
  var con: XMPPConnection = null
  @Before
  def setup {
    con = new XMPPConnection("localhost")
    assertNotNull(con)
    con.connect
  }
  @After
  def teardown {
    con match {
      case null => ()
      case _ => con.disconnect
    }
  }
  @Test
  def login() {
    var c = false
    con.login("user", "password")
    val chat = con.getChatManager.createChat("user'", new MessageListener {
      def processMessage(chat: Chat, message: Message): Unit = {
        c = true
      }
    })
    chat.sendMessage("hi")
    assertTrue("MessageListener not notified", c)
  }
}
