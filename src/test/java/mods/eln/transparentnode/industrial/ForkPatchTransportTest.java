package mods.eln.transparentnode.industrial;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.Test;
import static org.junit.Assert.*;

public class ForkPatchTransportTest {
    @Test public void boundedControlsRoundTripThroughTheActualNativeCodec() {
        for(int forward=-1;forward<=1;forward++)for(int turn=-1;turn<=1;turn++)for(int lift=-1;lift<=1;lift++)for(boolean pickup:new boolean[]{false,true}) {
            var input=new ForkliftControls(forward,turn,lift,pickup);
            var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
            try {
                ForkliftControls.CODEC.encode(buffer,input);
                assertEquals(4,buffer.readableBytes());
                assertEquals(input,ForkliftControls.CODEC.decode(buffer));
                assertEquals(0,buffer.readableBytes());
            }finally { buffer.release(); }
        }
    }
    @Test public void untrustedAndDirectInputsCannotExceedOneAxisUnit() {
        var direct=new ForkliftControls(Integer.MIN_VALUE,Integer.MAX_VALUE,100,true);
        assertEquals(new ForkliftControls(-1,1,1,true),direct);
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try {
            buffer.writeByte(-128);buffer.writeByte(127);buffer.writeByte(42);buffer.writeBoolean(false);
            assertEquals(new ForkliftControls(-1,1,1,false),ForkliftControls.CODEC.decode(buffer));
        }finally { buffer.release(); }
    }
    @Test public void truncatedPayloadsNeverProducePartiallyDecodedControls() {
        for(int length=0;length<4;length++) {
            var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
            try {
                for(int n=0;n<length;n++)buffer.writeByte(1);
                try { ForkliftControls.CODEC.decode(buffer);fail("Truncated payload returned controls: "+length); }
                catch(IndexOutOfBoundsException expected) { /* rejected before a handler can receive an input */ }
            }finally { buffer.release(); }
        }
    }
}
