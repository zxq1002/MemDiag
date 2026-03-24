package com.memdiag.core.nativeapi;

public class MemoryRegion {
    private final long startAddress;
    private final long endAddress;
    private final long residentSize;
    private final long size;
    private final String permissions;
    private final String mappingFile;
    private final String regionType;

    private MemoryRegion(Builder builder) {
        this.startAddress = builder.startAddress;
        this.endAddress = builder.endAddress;
        this.size = builder.size;
        this.residentSize = builder.residentSize;
        this.permissions = builder.permissions;
        this.mappingFile = builder.mappingFile;
        this.regionType = builder.regionType;
    }

    public long getStartAddress() { return startAddress; }
    public long getEndAddress() { return endAddress; }
    public long getSize() { return size; }
    public long getResidentSize() { return residentSize; }
    public String getPermissions() { return permissions; }
    public String getMappingFile() { return mappingFile; }
    public String getRegionType() { return regionType; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long startAddress;
        private long endAddress;
        private long size;
        private long residentSize;
        private String permissions;
        private String mappingFile;
        private String regionType;

        public Builder startAddress(long startAddress) {
            this.startAddress = startAddress;
            return this;
        }

        public Builder endAddress(long endAddress) {
            this.endAddress = endAddress;
            return this;
        }

        public Builder size(long size) {
            this.size = size;
            return this;
        }

        public Builder residentSize(long residentSize) {
            this.residentSize = residentSize;
            return this;
        }

        public Builder permissions(String permissions) {
            this.permissions = permissions;
            return this;
        }

        public Builder mappingFile(String mappingFile) {
            this.mappingFile = mappingFile;
            return this;
        }

        public Builder regionType(String regionType) {
            this.regionType = regionType;
            return this;
        }

        public MemoryRegion build() {
            return new MemoryRegion(this);
        }
    }
}
