package littleMaidMobX.network.packet.old;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import mmmlibx.lib.MMMLib;

public class Message implements IMessage
{
	/** チャネル番号 */
	public byte   ch;
	/** 実データ */
	public byte[] data;
	
	public Message(){}

	/** 
	 * @param ch どこ宛かを示すチャネル番号。LMM宛か、MMMLib宛かを区別するために追加した
	 * @param sendData 送信する実データ
	 *  */
	public Message(int ch, byte[] sendData)
	{
		this.ch		= (byte)ch;
		this.data	= sendData;
	}

	/** IMessageのメソッド。ByteBufからデータを読み取る。
	 * data[0] ... 通信パケットに勝手につくMOD側から見ればゴミ
	 * data[1] ... チャネル番号
	 * data[2] ... 以降が実データ
	 * */
	@Override
	public void fromBytes(ByteBuf buf)
	{
		int len = buf.array().length;
		if(len > 2)
		{
			this.data = new byte[len-2];
			this.ch =  buf.getByte(0);
			buf.getBytes(1, this.data);
		}
		else
		{
			this.data = new byte[]{0};
		}
	}

	@Override//IMessageのメソッド。ByteBufにデータを書き込む。
	public void toBytes(ByteBuf buf)
	{
		buf.writeByte(this.ch);
		buf.writeBytes(this.data);
	}

	public static class MessageHandler implements IMessageHandler<Message, IMessage> {

		@Override//IMessageHandlerのメソッド
		public IMessage onMessage(Message message, MessageContext ctx) {
			if (message.data != null) {
				if (ctx.side.isClient()) {
					//LittleMaidMobX.proxy.clientCustomPayload(message);
				} else {
					if (message.ch == 1) {
						MMMLib.serverCustomPayload(ctx.getServerHandler().playerEntity, message);
					}
					if (message.ch == 2) {
						//Net.serverCustomPayload(ctx.getServerHandler().playerEntity, message);
					}
				}
			}
			return null;//本来は返答用IMessageインスタンスを返すのだが、旧来のパケットの使い方をするなら必要ない。
		}
	}
}
