package me.marcdoesntexist.galaxymines.mines;




public record MineBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public int minChunkX() {
        return this.minX >> 4;
    }

    public int maxChunkX() {
        return this.maxX >> 4;
    }

    public int minChunkZ() {
        return this.minZ >> 4;
    }

    public int maxChunkZ() {
        return this.maxZ >> 4;
    }
}
