package io.github.puzzle.cosmic.impl.mixin.block;

import finalforeach.cosmicreach.blocks.Block;
import finalforeach.cosmicreach.util.Identifier;
import io.github.puzzle.cosmic.api.block.IPuzzleBlock;
import io.github.puzzle.cosmic.api.block.IPuzzleBlockState;
import io.github.puzzle.cosmic.api.util.IPuzzleIdentifier;
import io.github.puzzle.cosmic.util.annotation.Internal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Internal
@Mixin(Block.class)
public class BlockMixin implements IPuzzleBlock {

    @Unique
    private final transient Block puzzleLoader$block = IPuzzleBlock.as(this);

    @Override
    public IPuzzleBlockState pGetDefaultState() {
        return (IPuzzleBlockState) puzzleLoader$block.getDefaultBlockState();
    }

    @Override
    public BlockStateMap pGetStates() {
        return key -> (IPuzzleBlockState) puzzleLoader$block.blockStates.get(key);
    }

    @Override
    public IPuzzleIdentifier pGetIdentifier() {
        return IPuzzleIdentifier.as(Identifier.of(puzzleLoader$block.getStringId()));
    }

    @Override
    public String pGetName() {
        return puzzleLoader$block.getName();
    }

}
