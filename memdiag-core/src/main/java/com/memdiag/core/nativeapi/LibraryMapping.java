package com.memdiag.core.nativeapi;

public class LibraryMapping {
    private final long startAddress;
    private final long endAddress;
    private final String permissions;
    private final long offset;
    private final String device;
    private final long inode;
    private final String pathname;

    private LibraryMapping(Builder builder) {
        this.startAddress = builder.startAddress;
        this.endAddress = builder.endAddress;
        this.permissions = builder.permissions;
        this.offset = builder.offset;
        this.device = builder.device;
        this.inode = builder.inode;
        this.pathname = builder.pathname;
    }

    public long getStartAddress() { return startAddress; }
    public long getEndAddress() { return endAddress; }
    public String getPermissions() { return permissions; }
    public long getOffset() { return offset; }
    public String getDevice() { return device; }
    public long getInode() { return inode; }
    public String getPathname() { return pathname; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long startAddress;
        private long endAddress;
        private String permissions;
        private long offset;
        private String device;
        private long inode;
        private String pathname;

        public Builder startAddress(long startAddress) {
            this.startAddress = startAddress;
            return this;
        }

        public Builder endAddress(long endAddress) {
            this.endAddress = endAddress;
            return this;
        }

        public Builder permissions(String permissions) {
            this.permissions = permissions;
            return this;
        }

        public Builder offset(long offset) {
            this.offset = offset;
            return this;
        }

        public Builder device(String device) {
            this.device = device;
            return this;
        }

        public Builder inode(long inode) {
            this.inode = inode;
            return this;
        }

        public Builder pathname(String pathname) {
            this.pathname = pathname;
            return this;
        }

        public LibraryMapping build() {
            return new LibraryMapping(this);
        }
    }
}
