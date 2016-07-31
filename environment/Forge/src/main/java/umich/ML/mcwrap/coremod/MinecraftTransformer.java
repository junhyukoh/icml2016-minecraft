package umich.ML.mcwrap.coremod;

import umich.ML.mcwrap.event.RenderTickEventHandler;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Iterator;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

@SuppressWarnings("unused")
public class MinecraftTransformer implements IClassTransformer
{
    @Override
    public byte[] transform(String arg0, String arg1, byte[] arg2)
    {
        // Obfuscated mode
        if(arg0.equals("bsu"))
        {
            System.out.print("Inside obfuscated Minecraft Class. About to patch:" + arg0);
            return patchClassASM(arg0, arg2, true);
        }

        // Deobfuscated mode
        if (arg0.equals("net.minecraft.client.Minecraft"))
        {
            System.out.print("Inside deobfuscated Minecraft Class. About to patch: " + arg0);
            return patchClassASM(arg0, arg2, false);
        }

        return arg2;
    }

    public byte[] patchClassASM(String name, byte[] bytes, boolean obfuscated)
    {
        String targetMethodName;
        String updateDisplayMethodStr;

        if(obfuscated)
        {
            targetMethodName = "as";
            updateDisplayMethodStr = "h";
        }
        else
        {
            targetMethodName = "runGameLoop";
            updateDisplayMethodStr = "updateDisplay";
        }

        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(classNode, 0);

        for (MethodNode m : classNode.methods) {
            int updateDisplayInsnIndex = -1;

            if (m.name.equals(targetMethodName) && m.desc.equals("()V")) {
                System.out.println("Inside Method!");

                AbstractInsnNode currentNode;

                Iterator<AbstractInsnNode> iter = m.instructions.iterator();

                int index = -1;

                while (iter.hasNext()) {
                    index++;
                    currentNode = iter.next();

                    if (currentNode.getOpcode() == INVOKEVIRTUAL) {
                        if (((MethodInsnNode) currentNode).name.equals(updateDisplayMethodStr))
                            updateDisplayInsnIndex = index;
                    }
                }

                assert (updateDisplayInsnIndex >= 0);
                AbstractInsnNode aloadInsn = m.instructions.get(updateDisplayInsnIndex - 1);
                AbstractInsnNode updateDispInsn = m.instructions.get(updateDisplayInsnIndex);

                MethodInsnNode newUpdateDisplayInsn = new MethodInsnNode(INVOKESTATIC,
                        RenderTickEventHandler.class.getName().replace('.', '/'), "updateDisplay", "()V", false);

                m.instructions.insert(updateDispInsn, newUpdateDisplayInsn);

                m.instructions.remove(aloadInsn);
                m.instructions.remove(updateDispInsn);

                System.out.println("Patching Complete!");

                break;
            }
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        classNode.accept(writer);

        return writer.toByteArray();
    }
}
