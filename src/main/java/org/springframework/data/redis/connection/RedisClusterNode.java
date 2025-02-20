/*
 * Copyright 2015-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.redis.connection;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Representation of a Redis server within the cluster.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 1.7
 */
public class RedisClusterNode extends RedisNode {

	private SlotRange slotRange;
	private @Nullable LinkState linkState;
	private Set<Flag> flags;

	protected RedisClusterNode() {

		super();
		flags = Collections.emptySet();
	}

	/**
	 * Creates new {@link RedisClusterNode} with empty {@link SlotRange}.
	 *
	 * @param host must not be {@literal null}.
	 * @param port
	 */
	public RedisClusterNode(String host, int port) {
		this(host, port, SlotRange.empty());
	}

	/**
	 * Creates new {@link RedisClusterNode} with an id and empty {@link SlotRange}.
	 *
	 * @param id must not be {@literal null}.
	 */
	public RedisClusterNode(String id) {

		this(SlotRange.empty());
		Assert.notNull(id, "Id must not be null");
		this.id = id;
	}

	/**
	 * Creates new {@link RedisClusterNode} with given {@link SlotRange}.
	 *
	 * @param host must not be {@literal null}.
	 * @param port
	 * @param slotRange must not be {@literal null}.
	 */
	public RedisClusterNode(String host, int port, SlotRange slotRange) {

		super(host, port);

		Assert.notNull(slotRange, "SlotRange must not be null");
		this.slotRange = slotRange;
	}

	/**
	 * Creates new {@link RedisClusterNode} with given {@link SlotRange}.
	 *
	 * @param slotRange must not be {@literal null}.
	 */
	public RedisClusterNode(SlotRange slotRange) {

		super();

		Assert.notNull(slotRange, "SlotRange must not be null");

		this.slotRange = slotRange;
	}

	{
		if (flags == null) {
			flags = Collections.emptySet();
		}
	}

	/**
	 * Get the served {@link SlotRange}.
	 *
	 * @return never {@literal null}.
	 */
	public SlotRange getSlotRange() {
		return slotRange;
	}

	/**
	 * @param slot
	 * @return true if slot is covered.
	 */
	public boolean servesSlot(int slot) {
		return slotRange.contains(slot);
	}

	/**
	 * @return can be {@literal null}
	 */
	@Nullable
	public LinkState getLinkState() {
		return linkState;
	}

	/**
	 * @return true if node is connected to cluster.
	 */
	public boolean isConnected() {
		return LinkState.CONNECTED.equals(linkState);
	}

	/**
	 * @return never {@literal null}.
	 */
	public Set<Flag> getFlags() {
		return flags == null ? Collections.emptySet() : flags;
	}

	/**
	 * @return true if node is marked as failing.
	 */
	public boolean isMarkedAsFail() {

		if (!CollectionUtils.isEmpty(flags)) {
			return flags.contains(Flag.FAIL) || flags.contains(Flag.PFAIL);
		}
		return false;
	}

	@Override
	public String toString() {
		return super.toString();
	}

	/**
	 * Get {@link RedisClusterNodeBuilder} for creating new {@link RedisClusterNode}.
	 *
	 * @return never {@literal null}.
	 */
	public static RedisClusterNodeBuilder newRedisClusterNode() {
		return new RedisClusterNodeBuilder();
	}

	/**
	 * @author Christoph Strobl
	 * @author daihuabin
	 * @since 1.7
	 */
	public static class SlotRange {

		private final BitSet range;

		/**
		 * @param lowerBound must not be {@literal null}.
		 * @param upperBound must not be {@literal null}.
		 */
		public SlotRange(Integer lowerBound, Integer upperBound) {

			Assert.notNull(lowerBound, "LowerBound must not be null");
			Assert.notNull(upperBound, "UpperBound must not be null");

			this.range = new BitSet(upperBound + 1);
			for (int i = lowerBound; i <= upperBound; i++) {
				this.range.set(i);
			}
		}

		public SlotRange(Collection<Integer> range) {
			if (CollectionUtils.isEmpty(range)) {
				this.range = new BitSet(0);
			} else {
				this.range = new BitSet(ClusterSlotHashUtil.SLOT_COUNT);
				for (Integer pos : range) {
					this.range.set(pos);
				}
			}
		}

		public SlotRange(BitSet range) {
			this.range = (BitSet) range.clone();
		}

		@Override
		public String toString() {
			return Arrays.toString(this.getSlotsArray());
		}

		/**
		 * @param slot
		 * @return true when slot is part of the range.
		 */
		public boolean contains(int slot) {
			return range.get(slot);
		}

		/**
		 * @return
		 */
		public Set<Integer> getSlots() {
			if (range.isEmpty()) {
				return Collections.emptySet();
			}
			LinkedHashSet<Integer> slots = new LinkedHashSet<>(Math.max(2 * range.cardinality(), 11));
			for (int i = 0; i < range.length(); i++) {
				if (range.get(i)) {
					slots.add(i);
				}
			}
			return Collections.unmodifiableSet(slots);
		}

		public int[] getSlotsArray() {
			if (range.isEmpty()) {
				return new int[0];
			}
			int[] slots = new int[range.cardinality()];
			int pos = 0;

			for (int i = 0; i < ClusterSlotHashUtil.SLOT_COUNT; i++) {
				if (this.range.get(i)) {
					slots[pos++] = i;
				}
			}

			return slots;
		}

		public static SlotRange empty() {
			return new SlotRange(Collections.emptySet());
		}
	}

	/**
	 * @author Christoph Strobl
	 * @since 1.7
	 */
	public enum LinkState {
		CONNECTED, DISCONNECTED
	}

	/**
	 * @author Christoph Strobl
	 * @since 1.7
	 */
	public static enum Flag {

		MYSELF("myself"), MASTER("master"), REPLICA("slave"), FAIL("fail"), PFAIL("fail?"), HANDSHAKE("handshake"), NOADDR(
				"noaddr"), NOFLAGS("noflags");

		private String raw;

		Flag(String raw) {
			this.raw = raw;
		}

		public String getRaw() {
			return raw;
		}

	}

	/**
	 * Builder for creating new {@link RedisClusterNode}.
	 *
	 * @author Christoph Strobl
	 * @since 1.7
	 */
	public static class RedisClusterNodeBuilder extends RedisNodeBuilder {

		@Nullable Set<Flag> flags;
		@Nullable LinkState linkState;
		SlotRange slotRange;

		public RedisClusterNodeBuilder() {
			this.slotRange = SlotRange.empty();
		}

		@Override
		public RedisClusterNodeBuilder listeningAt(String host, int port) {
			super.listeningAt(host, port);
			return this;
		}

		@Override
		public RedisClusterNodeBuilder withName(String name) {
			super.withName(name);
			return this;
		}

		@Override
		public RedisClusterNodeBuilder withId(String id) {
			super.withId(id);
			return this;
		}

		@Override
		public RedisClusterNodeBuilder promotedAs(NodeType nodeType) {
			super.promotedAs(nodeType);
			return this;
		}

		@Override
		public RedisClusterNodeBuilder replicaOf(String masterId) {
			super.replicaOf(masterId);
			return this;
		}

		/**
		 * Set flags for node.
		 *
		 * @param flags
		 * @return
		 */
		public RedisClusterNodeBuilder withFlags(Set<Flag> flags) {

			this.flags = flags;
			return this;
		}

		/**
		 * Set {@link SlotRange}.
		 *
		 * @param range
		 * @return
		 */
		public RedisClusterNodeBuilder serving(SlotRange range) {

			this.slotRange = range;
			return this;
		}

		/**
		 * Set {@link LinkState}.
		 *
		 * @param linkState
		 * @return
		 */
		public RedisClusterNodeBuilder linkState(LinkState linkState) {
			this.linkState = linkState;
			return this;
		}

		@Override
		public RedisClusterNode build() {

			RedisNode base = super.build();

			RedisClusterNode node;
			if (base.host != null) {
				node = new RedisClusterNode(base.getHost(), base.getPort(), slotRange);
			} else {
				node = new RedisClusterNode(slotRange);
			}
			node.id = base.id;
			node.type = base.type;
			node.masterId = base.masterId;
			node.name = base.name;
			node.flags = flags;
			node.linkState = linkState;
			return node;
		}
	}

}
