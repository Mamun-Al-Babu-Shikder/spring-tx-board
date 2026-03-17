package com.sdlc.pro.txboard.redis;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public record RedisCommand(RedisInstruction instruction, byte[][] args) {

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
            var len = args.size();
            var bytes = new byte[len][];
            for (var i = 0; i < len; i++) {
                Object arg = args.get(i);
                if (arg instanceof byte[] byteArr) {
                    bytes[i] = byteArr;
                } else {
                    bytes[i] = arg.toString().getBytes();
                }
            }

            return bytes;
        }
    }
}
