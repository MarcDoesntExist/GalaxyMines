package me.marcdoesntexist.galaxymines.integration;

import me.marcdoesntexist.galaxymines.mines.Mine;




public interface MineRegionProtector {
    MineRegionProtector NOOP = new MineRegionProtector() {
        @Override
        public void reload() {
        }

        @Override
        public void onMineRegistered(final Mine mine) {
        }

        @Override
        public void onMineDeleted(final Mine mine) {
        }

        @Override
        public void onMineRenamed(final Mine mine, final String oldName) {
        }

        @Override
        public void close() {
        }
    };

    void reload();

    void onMineRegistered(Mine mine);

    void onMineDeleted(Mine mine);

    void onMineRenamed(Mine mine, String oldName);

    void close();
}
