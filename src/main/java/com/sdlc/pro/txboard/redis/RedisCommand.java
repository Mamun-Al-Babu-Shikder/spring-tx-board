package com.sdlc.pro.txboard.redis;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class RedisCommand {
    private final RedisInstruction instruction;
    private final byte[][] args;

    public RedisCommand(RedisInstruction instruction, byte[][] args) {
        this.instruction = instruction;
        this.args = args;
    }

    public RedisInstruction getInstruction() {
        return instruction;
    }

    public byte[][] getArgs() {
        return args;
    }

    public static Builder builder(RedisInstruction instruction) {
        return new Builder(instruction);
    }

    public static class Builder {
        private final RedisInstruction instruction;
        public final List<Object> args;

        private Builder(RedisInstruction instruction) {
            this.instruction = instruction;
            this.args = new LinkedList<>();
        }

        public Builder addArg(Object arg) {
            this.args.add(arg);
            return this;
        }

        public Builder addArgs(Object... args) {
            this.args.addAll(Arrays.asList(args));
            return this;
        }

        public RedisCommand build() {
            return new RedisCommand(this.instruction, convertToByteValue());
        }

        private byte[][] convertToByteValue() {
            int len = args.size();
            byte[][] bytes = new byte[len][];
            for (int i = 0; i < len; i++) {
                Object arg = args.get(i);
                if (arg instanceof byte[]) {
                    bytes[i] = (byte[]) arg;
                } else {
                    bytes[i] = arg.toString().getBytes();
                }
            }

            return bytes;
        }
    }
}
