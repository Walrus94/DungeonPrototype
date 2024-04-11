package org.dungeon.prototype.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.dungeon.prototype.model.Level;
import org.dungeon.prototype.model.Room;
import org.dungeon.prototype.model.Point;
import org.dungeon.prototype.model.ui.level.GridSection;
import org.dungeon.prototype.model.ui.level.WalkerIterator;
import org.springframework.stereotype.Component;

import java.util.*;

import static java.lang.Math.min;
import static java.lang.Math.round;
import static org.dungeon.prototype.model.Level.getOppsiteDirection;
import static org.dungeon.prototype.util.LevelUtil.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class LevelService {

}